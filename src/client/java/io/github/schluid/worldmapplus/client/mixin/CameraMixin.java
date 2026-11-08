package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves the camera above the player while the map is open. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private Entity entity;
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void worldmapplus$mapView(float partialTick, CallbackInfo ci) {
		if (!MapCamera.isActive() || entity == null) {
			return;
		}
		MapCamera.update();
		if (MapCamera.isFlatOnly()) {
			return;
		}
		Vec3 eye = entity.getEyePosition(partialTick);
		double pitch = MapCamera.pitch();
		double height = MapCamera.cameraHeight();
		double yaw = MapCamera.yaw();
		// in tilted view move back so the player stays in the middle
		double back = height * Math.cos(Math.toRadians(pitch)) / Math.sin(Math.toRadians(pitch));
		double backX = Math.sin(Math.toRadians(yaw)) * back;
		double backZ = -Math.cos(Math.toRadians(yaw)) * back;
		setPosition(eye.x + MapCamera.offsetX() + backX, eye.y + height, eye.z + MapCamera.offsetZ() + backZ);
		// yaw 180 = north up, pitch 90 = straight down
		setRotation((float) yaw, (float) pitch);
		// render the player model
		detached = true;
	}
}
