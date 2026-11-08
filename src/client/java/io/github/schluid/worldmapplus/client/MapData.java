package io.github.schluid.worldmapplus.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Keeps the 2D map up to date and handles surface/cave layers, saving and death points.
 */
public final class MapData {
	private static final MapData INSTANCE = new MapData();

	private static final int NEAR_INTERVAL = 10;
	private static final int SAVE_INTERVAL = 300;
	private static final int UNLOAD_INTERVAL = 100;
	private static final int REVEAL_INTERVAL = 5;
	private static final int MODE_INTERVAL = 10;
	public static final int LAYER_SIZE = 8;
	private static final int LAYER_MARGIN = 2;
	private static final int UNDERGROUND_DEPTH = 6;
	private static final int UNDERGROUND_SKY_LIGHT = 8;
	private static final int MAX_DEATH_POINTS = 20;
	private static final String DEATH_FILE = "deaths.txt";
	private static final int LAYERS_IN_MEMORY = 4;
	private static final int CAVE_FLOOR_DEPTH = 24;
	private static final int ROCK = 0xFF2A2A2E;
	private static final String SURFACE = "surface";

	private ClientLevel level;
	private ResourceKey<Level> dimension;
	private Path folder;
	private boolean ceiling;
	private final LinkedHashMap<String, MapLayer> layers = new LinkedHashMap<>(8, 0.75F, true);
	private MapLayer current;
	private int caveLayer = Integer.MIN_VALUE;
	private int scanIndex;
	private int tickCounter;
	private final List<int[]> deaths = new ArrayList<>();
	private boolean wasDead;
	private double lastRevealX;
	private double lastRevealZ;
	private int lastRevealRadius;
	private MapLayer lastRevealLayer;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	private MapData() {
	}

	public static MapData get() {
		return INSTANCE;
	}

	public List<MapTile> visibleTiles(double minX, double minZ, double maxX, double maxZ, int loadBudget) {
		return current == null ? Collections.emptyList() : current.visible(minX, minZ, maxX, maxZ, loadBudget);
	}

	public List<int[]> deathPoints(int count) {
		return deaths.subList(Math.max(0, deaths.size() - count), deaths.size());
	}

	public boolean isCaveMode() {
		return caveLayer != Integer.MIN_VALUE;
	}

	public void tick(Minecraft mc) {
		ClientLevel now = mc.level;
		if (now != level || now != null && now.dimension() != dimension) {
			saveAll();
			closeAll();
			level = now;
			dimension = now == null ? null : now.dimension();
			folder = now == null ? null : mapFolder(mc, now);
			ceiling = now != null && now.dimensionType().hasCeiling();
			caveLayer = Integer.MIN_VALUE;
			current = null;
			loadDeaths();
		}
		if (now == null || mc.player == null) {
			return;
		}
		try {
			boolean dead = mc.player.isDeadOrDying();
			if (dead && !wasDead) {
				addDeath(mc.player.blockPosition());
			}
			wasDead = dead;
			if (current == null || tickCounter % MODE_INTERVAL == 0) {
				updateMode(now, mc.player);
			}
			int centerX = mc.player.getBlockX() >> 4;
			int centerZ = mc.player.blockPosition().getZ() >> 4;
			int radius = Math.max(2, mc.options.getEffectiveRenderDistance());
			int side = radius * 2 + 1;
			int total = side * side;
			for (int i = 0; i < MapConfig.chunksPerTick; i++) {
				int index = scanIndex++ % total;
				scanChunk(now, centerX + index % side - radius, centerZ + index / side - radius);
			}
			if (scanIndex >= total) {
				scanIndex = 0;
			}
			if (++tickCounter % NEAR_INTERVAL == 0) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						scanChunk(now, centerX + dx, centerZ + dz);
					}
				}
			}
			if (tickCounter % REVEAL_INTERVAL == 0) {
				revealAround(mc.player.getX(), mc.player.getZ());
			}
			current.uploadChanges();
			if (tickCounter % UNLOAD_INTERVAL == 0) {
				int keep = radius / 16 + 2;
				for (MapLayer layer : layers.values()) {
					layer.unloadUnused(mc.player.getBlockX() >> MapTile.SHIFT,
							mc.player.blockPosition().getZ() >> MapTile.SHIFT, keep);
				}
			}
			if (tickCounter % SAVE_INTERVAL == 0) {
				saveAll();
			}
		} catch (RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not update map", e);
		}
	}

	private void updateMode(ClientLevel level, LocalPlayer player) {
		int feetY = player.blockPosition().getY();
		boolean underground = ceiling;
		if (!underground) {
			int x = player.getBlockX();
			int z = player.blockPosition().getZ();
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			int skyLight = level.getBrightness(LightLayer.SKY, player.blockPosition().above());
			underground = ground - (feetY + 2) >= UNDERGROUND_DEPTH && skyLight < UNDERGROUND_SKY_LIGHT;
		}
		int newLayer = Integer.MIN_VALUE;
		if (underground) {
			newLayer = Math.floorDiv(feetY, LAYER_SIZE);
			// small margin so stairs don't flip layers back and forth
			if (caveLayer != Integer.MIN_VALUE) {
				int bottom = caveLayer * LAYER_SIZE;
				if (feetY >= bottom - LAYER_MARGIN && feetY < bottom + LAYER_SIZE + LAYER_MARGIN) {
					newLayer = caveLayer;
				}
			}
		}
		if (current != null && newLayer == caveLayer) {
			return;
		}
		if (current != null) {
			current.save();
		}
		caveLayer = newLayer;
		String name = newLayer == Integer.MIN_VALUE ? SURFACE : "cave_" + newLayer * LAYER_SIZE;
		current = layers.get(name);
		if (current == null) {
			Path layerFolder = folder == null ? null
					: newLayer == Integer.MIN_VALUE ? folder : folder.resolve(name);
			current = new MapLayer(name, layerFolder);
			current.load();
			layers.put(name, current);
			evictOldLayers();
		}
		scanIndex = 0;
	}

	private void evictOldLayers() {
		Iterator<Map.Entry<String, MapLayer>> it = layers.entrySet().iterator();
		while (layers.size() > LAYERS_IN_MEMORY && it.hasNext()) {
			MapLayer layer = it.next().getValue();
			if (layer == current) {
				continue;
			}
			layer.save();
			layer.close();
			it.remove();
		}
	}

	private void scanChunk(ClientLevel level, int chunkX, int chunkZ) {
		LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null || chunk.isEmpty()) {
			return;
		}
		// north neighbour for the shading of the first row
		LevelChunk north = level.getChunkSource().getChunk(chunkX, chunkZ - 1, ChunkStatus.FULL, false);
		int minX = chunkX << 4;
		int minZ = chunkZ << 4;
		MapTile tile = current.tileFor(minX, minZ);
		for (int lx = 0; lx < 16; lx++) {
			int northHeight = north != null && !north.isEmpty() ? columnY(north, minX + lx, minZ - 1) : Integer.MIN_VALUE;
			for (int lz = 0; lz < 16; lz++) {
				int x = minX + lx;
				int z = minZ + lz;
				int height = columnY(chunk, x, z);
				int color = height == Integer.MIN_VALUE ? ROCK : columnColor(chunk, x, z, height, northHeight);
				tile.setPixel(x & (MapTile.SIZE - 1), z & (MapTile.SIZE - 1), color);
				northHeight = height;
			}
		}
	}

	private int columnY(LevelChunk chunk, int x, int z) {
		return isCaveMode() ? caveFloorY(chunk, x, z) : surfaceY(chunk, x, z);
	}

	private int surfaceY(LevelChunk chunk, int x, int z) {
		int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
		int minY = chunk.getMinY();
		for (int i = 0; i < 32 && y > minY; i++, y--) {
			BlockState state = chunk.getBlockState(pos.set(x, y, z));
			if (state.getMapColor(chunk, pos) != MapColor.NONE) {
				break;
			}
		}
		return y;
	}

	private int caveFloorY(LevelChunk chunk, int x, int z) {
		int minY = chunk.getMinY();
		int top = caveLayer * LAYER_SIZE + LAYER_SIZE - 1;
		int bottom = caveLayer * LAYER_SIZE;
		int y = top;
		while (y >= bottom && y > minY && isSolid(chunk, x, y, z)) {
			y--;
		}
		if (y < bottom || y <= minY) {
			return Integer.MIN_VALUE;
		}
		int lowest = Math.max(minY, bottom - CAVE_FLOOR_DEPTH);
		while (y > lowest && !isSolid(chunk, x, y, z)) {
			y--;
		}
		return y <= lowest ? Integer.MIN_VALUE : y;
	}

	private boolean isSolid(LevelChunk chunk, int x, int y, int z) {
		return !chunk.getBlockState(pos.set(x, y, z)).isAir();
	}

	private int columnColor(LevelChunk chunk, int x, int z, int y, int northHeight) {
		BlockState state = chunk.getBlockState(pos.set(x, y, z));
		MapColor mapColor = state.getMapColor(chunk, pos);
		if (mapColor == MapColor.NONE) {
			return isCaveMode() ? ROCK : 0;
		}
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty() && mapColor == MapColor.WATER) {
			int depth = 0;
			int minY = chunk.getMinY();
			while (depth < 24 && y - depth - 1 > minY
					&& !chunk.getBlockState(pos.set(x, y - depth - 1, z)).getFluidState().isEmpty()) {
				depth++;
			}
			MapColor.Brightness b = depth < 3 ? MapColor.Brightness.HIGH
					: depth < 8 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOW;
			return mapColor.calculateARGBColor(b);
		}
		MapColor.Brightness brightness = MapColor.Brightness.NORMAL;
		if (MapConfig.shading && northHeight != Integer.MIN_VALUE) {
			if (y > northHeight) {
				brightness = MapColor.Brightness.HIGH;
			} else if (y < northHeight) {
				brightness = MapColor.Brightness.LOW;
			}
		}
		return mapColor.calculateARGBColor(brightness);
	}

	private static Path mapFolder(Minecraft mc, ClientLevel level) {
		String world;
		IntegratedServer server = mc.getSingleplayerServer();
		ServerData data = mc.getCurrentServer();
		if (server != null) {
			Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			world = "singleplayer_" + root.getFileName();
		} else if (data != null) {
			world = "server_" + data.ip;
		} else {
			world = "unknown";
		}
		String dim = level.dimension().identifier().getNamespace() + "_" + level.dimension().identifier().getPath();
		return mc.gameDirectory.toPath().resolve(WorldMapPlusClient.MOD_ID).resolve(safe(world)).resolve(safe(dim));
	}

	private static String safe(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private void revealAround(double x, double z) {
		int radius = MapConfig.effectiveDiscoveryRadius();
		double dx = x - lastRevealX;
		double dz = z - lastRevealZ;
		if (current == lastRevealLayer && radius == lastRevealRadius && dx * dx + dz * dz < 4) {
			return;
		}
		current.reveal(x, z, radius);
		lastRevealX = x;
		lastRevealZ = z;
		lastRevealLayer = current;
		lastRevealRadius = radius;
	}

	private void addDeath(BlockPos p) {
		deaths.add(new int[] { p.getX(), p.getY(), p.getZ() });
		while (deaths.size() > MAX_DEATH_POINTS) {
			deaths.remove(0);
		}
		saveDeaths();
	}

	private void loadDeaths() {
		deaths.clear();
		wasDead = false;
		if (folder == null) {
			return;
		}
		Path file = folder.resolve(DEATH_FILE);
		if (!Files.exists(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file)) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length >= 3) {
					try {
						deaths.add(new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
								Integer.parseInt(parts[2]) });
					} catch (NumberFormatException ignored) {
						// ignore broken lines
					}
				}
			}
		} catch (IOException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not read death points", e);
		}
	}

	private void saveDeaths() {
		if (folder == null) {
			return;
		}
		StringBuilder text = new StringBuilder();
		for (int[] d : deaths) {
			text.append(d[0]).append(' ').append(d[1]).append(' ').append(d[2]).append('\n');
		}
		try {
			Files.createDirectories(folder);
			Files.writeString(folder.resolve(DEATH_FILE), text);
		} catch (IOException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not save death points", e);
		}
	}

	private void saveAll() {
		for (MapLayer layer : layers.values()) {
			layer.save();
		}
	}

	private void closeAll() {
		for (MapLayer layer : layers.values()) {
			layer.close();
		}
		layers.clear();
		scanIndex = 0;
	}
}
