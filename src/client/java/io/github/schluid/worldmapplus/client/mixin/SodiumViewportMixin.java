package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import io.github.schluid.worldmapplus.client.SodiumCullView;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium picks the chunk sections to draw around its viewport position (the camera), up to the
 * render distance, also in height. Our map camera hangs far above the player, so lower terrain
 * fell out of range and left holes. While the 3D map is open, the viewport also keeps the player's
 * position, and the other Sodium mixins use it only for choosing the sections. Which block faces
 * and what is in view still come from the real camera.
 * The values are fixed when the viewport is made, because Sodium culls on other threads.
 */
@Mixin(Viewport.class)
public abstract class SodiumViewportMixin implements SodiumCullView {
	@Unique
	private SectionPos worldmapplus$section;
	@Unique
	private BlockPos worldmapplus$block;
	@Unique
	private CameraTransform worldmapplus$transform;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void worldmapplus$init(Frustum frustum, Vector3d position, CallbackInfo ci) {
		Vec3 anchor = MapCamera.cullAnchor();
		if (anchor != null) {
			worldmapplus$block = BlockPos.containing(anchor);
			worldmapplus$section = SectionPos.of(worldmapplus$block);
			worldmapplus$transform = new CameraTransform(anchor.x, anchor.y, anchor.z);
		}
	}

	@Override
	public SectionPos worldmapplus$cullSection() {
		return worldmapplus$section;
	}

	@Override
	public BlockPos worldmapplus$cullBlock() {
		return worldmapplus$block;
	}

	@Override
	public CameraTransform worldmapplus$cullTransform() {
		return worldmapplus$transform;
	}
}
