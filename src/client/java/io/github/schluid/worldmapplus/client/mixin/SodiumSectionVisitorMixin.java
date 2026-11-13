package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.SodiumCullView;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.AbstractSectionVisitor;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sodium's lists of chosen sections cover the area around the player while the 3D map is open. */
@Mixin(AbstractSectionVisitor.class)
public abstract class SodiumSectionVisitorMixin {
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"))
	private SectionPos worldmapplus$center(Viewport viewport) {
		return SodiumCullView.section(viewport, viewport.getChunkCoord());
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;getBlockCoord()Lnet/minecraft/core/BlockPos;"))
	private BlockPos worldmapplus$centerBlock(Viewport viewport) {
		BlockPos anchor = ((SodiumCullView) (Object) viewport).worldmapplus$cullBlock();
		return anchor != null ? anchor : viewport.getBlockCoord();
	}
}
