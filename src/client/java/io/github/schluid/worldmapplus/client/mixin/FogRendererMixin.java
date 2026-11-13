package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No fog in the map view. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	private static final float FAR = 1.0E6F;

	@Inject(method = "setupFog", at = @At("RETURN"))
	private void worldmapplus$noFog(CallbackInfoReturnable<FogData> cir) {
		if (!MapCamera.isActive() || MapCamera.isFlatOnly()) {
			return;
		}
		FogData fog = cir.getReturnValue();
		if (fog != null) {
			fog.environmentalStart = FAR;
			fog.environmentalEnd = FAR;
			// looking at the horizon: keep the normal fog at the edge of the loaded world
			if (!MapCamera.horizonVisible()) {
				fog.renderDistanceStart = FAR;
				fog.renderDistanceEnd = FAR;
			}
		}
	}
}
