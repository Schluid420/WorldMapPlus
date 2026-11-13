package io.github.schluid.worldmapplus.client;

import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * Added to Sodium's viewport: while the 3D map is open, the position (the player) that Sodium
 * chooses its chunk sections around. All values are null for the normal view.
 * Only loaded when Sodium is installed.
 */
public interface SodiumCullView {
	SectionPos worldmapplus$cullSection();

	BlockPos worldmapplus$cullBlock();

	CameraTransform worldmapplus$cullTransform();

	static SectionPos section(Object viewport, SectionPos camera) {
		SectionPos anchor = ((SodiumCullView) viewport).worldmapplus$cullSection();
		return anchor != null ? anchor : camera;
	}
}
