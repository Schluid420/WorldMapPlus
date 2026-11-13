package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.RayOcclusionSectionTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium hides sections that rays from the camera can't reach. From the far-away map camera this
 * leaves holes, so it is switched off while the 3D map is open (like the cave culling in vanilla).
 */
@Mixin(RayOcclusionSectionTree.class)
public abstract class SodiumRayOcclusionMixin {
	@Inject(method = "isRayBlockedStepped", at = @At("HEAD"), cancellable = true)
	private void worldmapplus$notBlocked(RenderSection section, CallbackInfoReturnable<Boolean> cir) {
		if (MapCamera.cullAnchor() != null) {
			cir.setReturnValue(false);
		}
	}
}
