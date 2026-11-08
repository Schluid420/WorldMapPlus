package io.github.schluid.worldmapplus.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** 256x256 block piece of the map with its discovery mask. */
public final class MapTile {
	public static final int SIZE = 256;
	public static final int SHIFT = 8;
	public static final int FOG_CELL = 4;
	public static final int FOG_SIZE = SIZE / FOG_CELL;
	private static final int FOG_HIDDEN = 0xFF000000;
	private static final int FOG_OPEN = 0x00000000;

	private static int counter;

	public final int tileX;
	public final int tileZ;
	public final Identifier id;
	public final Identifier fogId;
	private final DynamicTexture texture;
	private final DynamicTexture fog;
	private boolean fogDirty;
	private boolean fogUnsaved;
	long lastUsed = System.currentTimeMillis();
	private boolean dirty;
	private boolean unsaved;

	MapTile(int tileX, int tileZ) {
		this(tileX, tileZ, null, null);
	}

	MapTile(int tileX, int tileZ, NativeImage loaded, NativeImage loadedFog) {
		this.tileX = tileX;
		this.tileZ = tileZ;
		int number = counter++;
		this.id = Identifier.fromNamespaceAndPath(WorldMapPlusClient.MOD_ID, "map_tile_" + number);
		this.fogId = Identifier.fromNamespaceAndPath(WorldMapPlusClient.MOD_ID, "map_fog_" + number);
		NativeImage pixels = new NativeImage(SIZE, SIZE, true);
		if (loaded != null) {
			int w = Math.min(SIZE, loaded.getWidth());
			int h = Math.min(SIZE, loaded.getHeight());
			for (int z = 0; z < h; z++) {
				for (int x = 0; x < w; x++) {
					pixels.setPixel(x, z, loaded.getPixel(x, z));
				}
			}
		}
		this.texture = new DynamicTexture(() -> "WorldMapPlus tile " + tileX + "," + tileZ, pixels);
		Minecraft.getInstance().getTextureManager().register(id, texture);
		texture.upload();

		NativeImage fogPixels = new NativeImage(FOG_SIZE, FOG_SIZE, false);
		fogPixels.fillRect(0, 0, FOG_SIZE, FOG_SIZE, FOG_HIDDEN);
		if (loadedFog != null) {
			int w = Math.min(FOG_SIZE, loadedFog.getWidth());
			int h = Math.min(FOG_SIZE, loadedFog.getHeight());
			for (int z = 0; z < h; z++) {
				for (int x = 0; x < w; x++) {
					fogPixels.setPixel(x, z, loadedFog.getPixel(x, z));
				}
			}
		}
		this.fog = new DynamicTexture(() -> "WorldMapPlus fog " + tileX + "," + tileZ, fogPixels);
		Minecraft.getInstance().getTextureManager().register(fogId, fog);
		fog.upload();
	}

	String fogFileName() {
		return tileX + "_" + tileZ + "_fog.png";
	}

	void reveal(int cellX, int cellZ) {
		NativeImage pixels = fog.getPixels();
		if (pixels != null && pixels.getPixel(cellX, cellZ) != FOG_OPEN) {
			pixels.setPixel(cellX, cellZ, FOG_OPEN);
			fogDirty = true;
			fogUnsaved = true;
		}
	}

	String fileName() {
		return tileX + "_" + tileZ + ".png";
	}

	void setPixel(int x, int z, int argb) {
		NativeImage pixels = texture.getPixels();
		if (pixels != null && pixels.getPixel(x, z) != argb) {
			pixels.setPixel(x, z, argb);
			dirty = true;
			unsaved = true;
		}
	}

	void uploadIfDirty() {
		if (dirty) {
			dirty = false;
			texture.upload();
		}
		if (fogDirty) {
			fogDirty = false;
			fog.upload();
		}
	}

	/** Returns true if a file was written. */
	boolean saveIfChanged(Path folder) throws IOException {
		boolean written = false;
		NativeImage pixels = texture.getPixels();
		if (unsaved && pixels != null) {
			PngWriter.write(pixels, folder.resolve(fileName()));
			unsaved = false;
			written = true;
		}
		NativeImage fogPixels = fog.getPixels();
		if (fogUnsaved && fogPixels != null) {
			PngWriter.write(fogPixels, folder.resolve(fogFileName()));
			fogUnsaved = false;
			written = true;
		}
		return written;
	}

	void close() {
		Minecraft.getInstance().getTextureManager().release(id);
		Minecraft.getInstance().getTextureManager().release(fogId);
	}
}
