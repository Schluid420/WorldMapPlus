package io.github.schluid.worldmapplus.client.mixin;

import io.github.schluid.worldmapplus.client.MapCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.levelgen.Heightmap;
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
	private static final double GROUND_CLEARANCE = 1.5;

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
			MapCamera.updateCulling();
			return;
		}
		MapCamera.update();
		MapCamera.updateCulling();
		if (MapCamera.isFlatOnly()) {
			return;
		}
		Vec3 eye = entity.getEyePosition(partialTick);
		MapCamera.setAnchor(eye);
		double pitch = MapCamera.pitch();
		// the camera stays at the same distance from the player and only swings around him,
		// so tilting doesn't zoom
		double distance = MapCamera.cameraHeight();
		double height = distance * Math.sin(Math.toRadians(pitch));
		double back = distance * Math.cos(Math.toRadians(pitch));
		double yaw = MapCamera.yaw();
		double backX = Math.sin(Math.toRadians(yaw)) * back;
		double backZ = -Math.cos(Math.toRadians(yaw)) * back;
		double x = eye.x + MapCamera.offsetX() + backX;
		double y = eye.y + height;
		double z = eye.z + MapCamera.offsetZ() + backZ;
		// never inside the ground: lift the camera above the terrain below it
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
			y = Math.max(y, ground + GROUND_CLEARANCE);
		}
		setPosition(x, y, z);
		// yaw 180 = north up, pitch 90 = straight down
		setRotation((float) yaw, (float) pitch);
		// render the player model
		detached = true;
	}
}
