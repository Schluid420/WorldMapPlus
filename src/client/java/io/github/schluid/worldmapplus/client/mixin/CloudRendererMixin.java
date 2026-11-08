package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import io.github.schluid.worldmapplus.client.MapConfig;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides clouds in the map view. */
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
	@Inject(method = {
			"render(Lnet/minecraft/client/CloudStatus;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
			"renderOit" }, at = @At("HEAD"), cancellable = true, require = 1)
	private void worldmapplus$hideClouds(CallbackInfo ci) {
		if (MapCamera.isActive() && !MapCamera.isFlatOnly() && MapConfig.hideClouds) {
			ci.cancel();
		}
	}
}
