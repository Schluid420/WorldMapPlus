package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.SodiumCullView;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium uses an emergency path when the camera is outside the loaded chunks. The map camera
 * often is, so while the 3D map is open this is checked for the player instead.
 */
@Mixin(RenderSectionManager.class)
public abstract class SodiumRenderSectionManagerMixin {
	@Redirect(method = {"prepareRenderTrees", "readRenderListFromTree"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"))
	private SectionPos worldmapplus$center(Viewport viewport) {
		return SodiumCullView.section(viewport, viewport.getChunkCoord());
	}
}
