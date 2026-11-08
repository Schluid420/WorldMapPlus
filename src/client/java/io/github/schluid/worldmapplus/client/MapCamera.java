package io.github.schluid.worldmapplus.client;

import net.minecraft.client.Minecraft;

/** Camera state for the map view: height, tilt, rotation and panning. */
public final class MapCamera {
	public static final double MIN_HEIGHT = 8;
	private static final double TILT_DEGREES = 30;
	private static final double FLAT_RANGE = 16;

	private static boolean active;
	private static boolean flatOnly;
	private static boolean returning;
	private static double height;
	private static double targetHeight;
	private static double offsetX;
	private static double offsetZ;
	private static boolean movedIn2d;
	private static long lastNanos;
	private static double tilt;
	private static double tiltTarget;
	private static double rotation;
	private static double spin;

	private MapCamera() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void open(boolean flatOnlyView) {
		active = true;
		flatOnly = flatOnlyView;
		returning = false;
		height = 0;
		targetHeight = Math.min(MapConfig.startHeight, maxHeight());
		offsetX = 0;
		offsetZ = 0;
		rotation = 0;
		spin = 0;
		lastNanos = System.nanoTime();
	}

	public static void close() {
		returning = true;
		targetHeight = 0;
	}

	public static double switchHeight() {
		double percent = 1.0 - (1.0 - MapConfig.tiltSwitchPercent / 100.0) * tilt;
		return Math.max(MIN_HEIGHT * 4, MapConfig.max3dHeight * percent);
	}

	public static double maxHeight() {
		return MapConfig.flatMap || flatOnly ? switchHeight() * FLAT_RANGE : switchHeight();
	}

	public static void zoom(double scroll) {
		if (!returning) {
			double factor = 1 - MapConfig.zoomSpeed * 0.04;
			targetHeight = Math.clamp(targetHeight * Math.pow(factor, scroll), MIN_HEIGHT, maxHeight());
		}
	}

	public static void clampToRange() {
		targetHeight = Math.min(targetHeight, maxHeight());
	}

	public static void pan(double blocksX, double blocksZ) {
		if (!returning) {
			offsetX += blocksX;
			offsetZ += blocksZ;
			if (flatAlpha() > 0.5F) {
				movedIn2d = true;
			}
		}
	}

	public static void rotate(double degrees) {
		if (!returning) {
			rotation = ((rotation + degrees) % 360 + 360) % 360;
		}
	}

	public static void resetRotation() {
		rotation = 0;
		spin = 0;
	}

	public static void setSpin(double degreesPerSecond) {
		spin = MapConfig.rotateInertia == 0 ? 0 : Math.clamp(degreesPerSecond, -720.0, 720.0);
	}

	public static double yaw() {
		return 180.0 + rotation;
	}

	public static void toggleTilt() {
		tiltTarget = tiltTarget > 0.5 ? 0 : 1;
	}

	public static double pitch() {
		return 90.0 - TILT_DEGREES * tilt * (1.0 - flatAlpha());
	}

	public static void resetPan() {
		offsetX = 0;
		offsetZ = 0;
		movedIn2d = false;
	}

	public static double offsetX() {
		return offsetX;
	}

	public static double offsetZ() {
		return offsetZ;
	}

	public static double currentHeight() {
		return height;
	}

	public static double targetHeight() {
		return targetHeight;
	}

	public static boolean isFlatOnly() {
		return flatOnly;
	}

	public static double cameraHeight() {
		return Math.min(height, switchHeight());
	}

	public static float flatAlpha() {
		if (flatOnly) {
			return 1;
		}
		if (!MapConfig.flatMap) {
			return 0;
		}
		double s = switchHeight();
		double from = s * 0.8;
		return (float) Math.clamp((height - from) / (s - from), 0.0, 1.0);
	}

	public static void update() {
		long now = System.nanoTime();
		double seconds = Math.min(0.1, (now - lastNanos) / 1.0E9);
		lastNanos = now;
		double step = 1 - Math.exp(-seconds * MapConfig.flightSpeed);
		height += (targetHeight - height) * step;
		tilt += (tiltTarget - tilt) * step;
		if (spin != 0) {
			rotate(spin * seconds);
			spin *= Math.exp(-seconds * (11 - MapConfig.rotateInertia) * 0.8);
			if (Math.abs(spin) < 2) {
				spin = 0;
			}
		}
		if (returning) {
			offsetX -= offsetX * step;
			offsetZ -= offsetZ * step;
			if (height < 0.5) {
				active = false;
				returning = false;
				height = 0;
				resetPan();
			}
			return;
		}
		// panned on the 2D map -> recenter before 3D comes back
		if (movedIn2d && targetHeight < height && height < switchHeight() * 1.3) {
			double pull = Math.min(1.0, step * 3);
			offsetX -= offsetX * pull;
			offsetZ -= offsetZ * pull;
			if (Math.abs(offsetX) < 0.5 && Math.abs(offsetZ) < 0.5) {
				resetPan();
			}
		}
		// keep the 3D view inside loaded chunks
		if (height < switchHeight()) {
			double limit = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
			double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
			if (distance > limit) {
				double factor = 1 - (1 - limit / distance) * step;
				offsetX *= factor;
				offsetZ *= factor;
			}
		}
	}
}
