package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Picks the chunk sections to draw around the player instead of the far-away map camera. */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
	@Redirect(method = "*", at = @At(value = "FIELD", opcode = 180,
			target = "Lnet/minecraft/client/renderer/state/level/CameraRenderState;pos:Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 worldmapplus$sectionCenter(CameraRenderState state) {
		return MapCamera.sectionCenter(state.pos);
	}

	@Redirect(method = "*", at = @At(value = "FIELD", opcode = 180,
			target = "Lnet/minecraft/client/renderer/state/level/CameraRenderState;blockPos:Lnet/minecraft/core/BlockPos;"))
	private BlockPos worldmapplus$sectionCenterBlock(CameraRenderState state) {
		return MapCamera.sectionCenter(state.blockPos);
	}
}
