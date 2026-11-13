package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.TraversableTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When Sodium makes its list of sections to draw, it checks the distance to the camera once more.
 * While the 3D map is open, the list only holds sections around the player anyway, so that check
 * is skipped. The check whether a section is in view stays.
 */
@Mixin(TraversableTree.class)
public abstract class SodiumTraversableTreeMixin {
	@Inject(method = "cylindricalDistanceTest", at = @At("HEAD"), cancellable = true)
	private static void worldmapplus$inRange(float x, float y, float z, float limit, CallbackInfoReturnable<Boolean> cir) {
		if (MapCamera.cullAnchor() != null) {
			cir.setReturnValue(true);
		}
	}
}
