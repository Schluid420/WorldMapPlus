package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import io.github.schluid.worldmapplus.client.SodiumCullView;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.AbstractSectionVisitor;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A list of chosen sections stays valid as long as the player (not the map camera) stays near.
 * Sodium also keeps a small list that was only searched inside the current view. Seen from the
 * far-away map camera that search misses sections (holes, see-through terrain after turning),
 * so while the 3D map is open only the full lists are used; what is in view is checked when drawing.
 */
@Mixin(SectionTree.class)
public abstract class SodiumSectionTreeMixin extends AbstractSectionVisitor {
	private SodiumSectionTreeMixin() {
		super(null, 0, false);
	}

	@Inject(method = "isValidFor", at = @At("HEAD"), cancellable = true)
	private void worldmapplus$onlyFullLists(Viewport viewport, float distance, CallbackInfoReturnable<Boolean> cir) {
		if (isFrustumTested && MapCamera.cullAnchor() != null) {
			cir.setReturnValue(false);
		}
	}

	@Redirect(method = "isValidFor", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"))
	private SectionPos worldmapplus$center(Viewport viewport) {
		return SodiumCullView.section(viewport, viewport.getChunkCoord());
	}
}
