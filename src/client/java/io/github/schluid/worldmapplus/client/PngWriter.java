package io.github.schluid.worldmapplus.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** Writes palette PNGs, much smaller than normal RGBA PNGs for map tiles. */
final class PngWriter {
	private static final byte[] SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

	private PngWriter() {
	}

	static void write(NativeImage image, Path file) throws IOException {
		int width = image.getWidth();
		int height = image.getHeight();
		Map<Integer, Integer> index = new HashMap<>();
		int[] palette = new int[256];
		byte[] rows = new byte[(width + 1) * height];
		int p = 0;
		for (int y = 0; y < height; y++) {
			rows[p++] = 0; // filter: none
			for (int x = 0; x < width; x++) {
				int argb = image.getPixel(x, y);
				if ((argb >>> 24) == 0) {
					argb = 0;
				}
				Integer i = index.get(argb);
				if (i == null) {
					if (index.size() == 256) {
						// too many colors, fall back to a normal PNG
						image.writeToFile(file);
						return;
					}
					i = index.size();
					index.put(argb, i);
					palette[i] = argb;
				}
				rows[p++] = (byte) (int) i;
			}
		}
		int colors = Math.max(1, index.size());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(SIGNATURE);

		ByteArrayOutputStream header = new ByteArrayOutputStream();
		DataOutputStream h = new DataOutputStream(header);
		h.writeInt(width);
		h.writeInt(height);
		h.writeByte(8); // bit depth
		h.writeByte(3); // color type: palette
		h.writeByte(0);
		h.writeByte(0);
		h.writeByte(0);
		chunk(out, "IHDR", header.toByteArray());

		byte[] plte = new byte[colors * 3];
		byte[] trns = new byte[colors];
		for (int i = 0; i < colors; i++) {
			int c = palette[i];
			plte[i * 3] = (byte) (c >> 16);
			plte[i * 3 + 1] = (byte) (c >> 8);
			plte[i * 3 + 2] = (byte) c;
			trns[i] = (byte) (c >>> 24);
		}
		chunk(out, "PLTE", plte);
		chunk(out, "tRNS", trns);

		ByteArrayOutputStream packed = new ByteArrayOutputStream();
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try (DeflaterOutputStream z = new DeflaterOutputStream(packed, deflater)) {
			z.write(rows);
		} finally {
			deflater.end();
		}
		chunk(out, "IDAT", packed.toByteArray());
		chunk(out, "IEND", new byte[0]);

		// write to a temp file first so a crash can't leave a broken tile
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.write(temp, out.toByteArray());
		Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
	}

	private static void chunk(OutputStream out, String type, byte[] data) throws IOException {
		DataOutputStream d = new DataOutputStream(out);
		byte[] name = type.getBytes(StandardCharsets.US_ASCII);
		d.writeInt(data.length);
		d.write(name);
		d.write(data);
		CRC32 crc = new CRC32();
		crc.update(name);
		crc.update(data);
		d.writeInt((int) crc.getValue());
	}
}
