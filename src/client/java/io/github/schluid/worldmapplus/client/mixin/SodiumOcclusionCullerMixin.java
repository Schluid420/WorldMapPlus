package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.SodiumCullView;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium's chunk search starts at the camera and measures the distance of each section from it.
 * While the 3D map is open, it starts at the player and measures from there (see SodiumViewportMixin).
 */
@Mixin(OcclusionCuller.class)
public abstract class SodiumOcclusionCullerMixin {
	@Redirect(method = {"findVisible", "processQueue", "addNearbySections"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"))
	private SectionPos worldmapplus$start(Viewport viewport) {
		return SodiumCullView.section(viewport, viewport.getChunkCoord());
	}

	@Redirect(method = {"getAngleVisibilityMaskWide", "getDirectionSetsWide"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"))
	private static SectionPos worldmapplus$startStatic(Viewport viewport) {
		return SodiumCullView.section(viewport, viewport.getChunkCoord());
	}

	@Redirect(method = "visitNode", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getTransform()Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;"))
	private CameraTransform worldmapplus$distanceFrom(Viewport viewport) {
		CameraTransform anchor = ((SodiumCullView) (Object) viewport).worldmapplus$cullTransform();
		return anchor != null ? anchor : viewport.getTransform();
	}
}
