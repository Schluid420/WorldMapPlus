package io.github.schluid.worldmapplus.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One map layer (surface or a cave layer) and its tiles on disk. Tiles are only loaded when they
 * are needed and unloaded again after a while, so big maps don't fill up the graphics memory.
 */
final class MapLayer {
	private static final Pattern TILE_FILE = Pattern.compile("^(-?\\d+)_(-?\\d+)(_fog)?\\.png$");
	private static final long UNLOAD_AFTER_MS = 20_000;

	final String name;
	private final Path folder;
	private final Map<Long, MapTile> tiles = new HashMap<>();
	/** Tiles that exist on disk (loaded or not). */
	private final Set<Long> onDisk = new HashSet<>();

	MapLayer(String name, Path folder) {
		this.name = name;
		this.folder = folder;
	}

	private static long key(int tileX, int tileZ) {
		return ((long) tileX << 32) | (tileZ & 0xFFFFFFFFL);
	}

	int loadedCount() {
		return tiles.size();
	}

	MapTile tileFor(int blockX, int blockZ) {
		int tx = blockX >> MapTile.SHIFT;
		int tz = blockZ >> MapTile.SHIFT;
		long k = key(tx, tz);
		MapTile tile = tiles.get(k);
		if (tile == null) {
			tile = onDisk.contains(k) ? loadTile(tx, tz) : null;
			if (tile == null) {
				tile = new MapTile(tx, tz);
			}
			tiles.put(k, tile);
		}
		tile.lastUsed = System.currentTimeMillis();
		return tile;
	}

	/**
	 * Tiles inside the given block area, for drawing. Missing tiles that exist on disk are loaded,
	 * but at most {@code loadBudget} per call, so zooming out doesn't freeze the game.
	 */
	List<MapTile> visible(double minX, double minZ, double maxX, double maxZ, int loadBudget) {
		int minTx = (int) Math.floor(minX) >> MapTile.SHIFT;
		int minTz = (int) Math.floor(minZ) >> MapTile.SHIFT;
		int maxTx = (int) Math.floor(maxX) >> MapTile.SHIFT;
		int maxTz = (int) Math.floor(maxZ) >> MapTile.SHIFT;
		List<MapTile> result = new ArrayList<>();
		long now = System.currentTimeMillis();
		for (int tz = minTz; tz <= maxTz; tz++) {
			for (int tx = minTx; tx <= maxTx; tx++) {
				long k = key(tx, tz);
				MapTile tile = tiles.get(k);
				if (tile == null && loadBudget > 0 && onDisk.contains(k)) {
					loadBudget--;
					tile = loadTile(tx, tz);
					if (tile != null) {
						tiles.put(k, tile);
					}
				}
				if (tile != null) {
					tile.lastUsed = now;
					result.add(tile);
				}
			}
		}
		return result;
	}

	void uploadChanges() {
		for (MapTile tile : tiles.values()) {
			tile.uploadIfDirty();
		}
	}

	/** Remembers which tiles are saved. The images themselves are loaded later when needed. */
	void load() {
		if (folder == null || !Files.isDirectory(folder)) {
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.png")) {
			for (Path file : files) {
				Matcher m = TILE_FILE.matcher(file.getFileName().toString());
				if (m.matches()) {
					onDisk.add(key(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
				}
			}
		} catch (IOException | RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not read map folder " + folder, e);
		}
		WorldMapPlusClient.LOGGER.info("WorldMapPlus: found " + onDisk.size() + " map tiles in " + folder);
	}

	private MapTile loadTile(int tx, int tz) {
		NativeImage image = read(folder.resolve(tx + "_" + tz + ".png"));
		NativeImage fog = read(folder.resolve(tx + "_" + tz + "_fog.png"));
		try {
			return new MapTile(tx, tz, image, fog);
		} catch (RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not load map tile " + tx + "," + tz, e);
			return null;
		} finally {
			if (image != null) {
				image.close();
			}
			if (fog != null) {
				fog.close();
			}
		}
	}

	private static NativeImage read(Path file) {
		if (file == null || !Files.exists(file)) {
			return null;
		}
		try (InputStream in = Files.newInputStream(file)) {
			return NativeImage.read(in);
		} catch (IOException | RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not read " + file, e);
			return null;
		}
	}

	/**
	 * Saves and frees tiles that weren't used for a while and are not close to the player.
	 * Tiles right around the player always stay, they are updated all the time.
	 */
	void unloadUnused(int playerTileX, int playerTileZ, int keepRadius) {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<Long, MapTile>> it = tiles.entrySet().iterator();
		while (it.hasNext()) {
			MapTile tile = it.next().getValue();
			boolean near = Math.abs(tile.tileX - playerTileX) <= keepRadius
					&& Math.abs(tile.tileZ - playerTileZ) <= keepRadius;
			if (near || now - tile.lastUsed < UNLOAD_AFTER_MS) {
				continue;
			}
			if (saveTile(tile)) {
				tile.close();
				it.remove();
			}
		}
	}

	void reveal(double centerX, double centerZ, int radius) {
		int cell = MapTile.FOG_CELL;
		int minCellX = (int) Math.floor((centerX - radius) / cell);
		int maxCellX = (int) Math.floor((centerX + radius) / cell);
		int minCellZ = (int) Math.floor((centerZ - radius) / cell);
		int maxCellZ = (int) Math.floor((centerZ + radius) / cell);
		double r2 = (double) radius * radius;
		for (int cz = minCellZ; cz <= maxCellZ; cz++) {
			double dz = (cz + 0.5) * cell - centerZ;
			for (int cx = minCellX; cx <= maxCellX; cx++) {
				double dx = (cx + 0.5) * cell - centerX;
				if (dx * dx + dz * dz > r2) {
					continue;
				}
				MapTile tile = tileFor(cx * cell, cz * cell);
				tile.reveal(Math.floorMod(cx, MapTile.FOG_SIZE), Math.floorMod(cz, MapTile.FOG_SIZE));
			}
		}
	}

	/** Saves one tile if it changed. Returns false if that failed (then it stays loaded). */
	private boolean saveTile(MapTile tile) {
		if (folder == null) {
			return true;
		}
		try {
			Files.createDirectories(folder);
			if (tile.saveIfChanged(folder)) {
				onDisk.add(key(tile.tileX, tile.tileZ));
			}
			return true;
		} catch (IOException | RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not save map to " + folder, e);
			return false;
		}
	}

	void save() {
		for (MapTile tile : tiles.values()) {
			if (!saveTile(tile)) {
				return;
			}
		}
	}

	void close() {
		for (MapTile tile : tiles.values()) {
			tile.close();
		}
		tiles.clear();
	}
}
