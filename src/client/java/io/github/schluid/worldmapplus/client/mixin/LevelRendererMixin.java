package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the grid of chunk sections centered on the player while the map camera is far away. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Redirect(method = "repositionCamera", at = @At(value = "FIELD", opcode = 180,
			target = "Lnet/minecraft/client/renderer/state/level/CameraRenderState;pos:Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 worldmapplus$gridCenter(CameraRenderState state) {
		return MapCamera.sectionCenter(state.pos);
	}
}
