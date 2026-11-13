package io.github.schluid.worldmapplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Camera state for the map view: height, tilt, rotation and panning. */
public final class MapCamera {
	public static final double MIN_HEIGHT = 8;
	private static final double DEFAULT_TILT = 30;
	/** The top screen edge always looks at least this far down, so the camera never sees the horizon. */
	private static final double MIN_TOP_EDGE_ANGLE = 4;
	public static final double MIN_TILT = 10;
	public static final double MAX_TILT = 80;
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
	// degrees away from straight down, 0 = straight down
	private static double tilt;
	private static double tiltTarget;
	private static double lastTilt = DEFAULT_TILT;
	private static boolean cullingOff;
	private static volatile Vec3 anchor;
	private static boolean previousSmartCull;
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
		return Math.max(MIN_HEIGHT * 4, MapConfig.max3dHeight);
	}

	public static double maxHeight() {
		return MapConfig.flatMap || flatOnly ? switchHeight() * FLAT_RANGE : switchHeight();
	}

	/**
	 * Furthest camera distance for a tilt: the top edge of the screen must still hit the ground
	 * inside the loaded chunks. The flatter the view, the closer the camera has to stay.
	 */
	static double tiltedMaxDistance(double tiltDegrees) {
		Minecraft mc = Minecraft.getInstance();
		double reach = mc.options.getEffectiveRenderDistance() * 16.0 * 0.9;
		double halfFov = mc.options.fov().get() / 2.0;
		// steepest tilt where the top screen edge still hits the ground
		double horizonTilt = 90.0 - halfFov - MIN_TOP_EDGE_ANGLE;
		double max = reach / groundReach(Math.min(tiltDegrees, horizonTilt), halfFov);
		if (tiltDegrees > horizonTilt) {
			// the horizon is in view now: stay close, vanilla fog hides the edge of the world
			double t = (tiltDegrees - horizonTilt) / Math.max(1.0, MAX_TILT - horizonTilt);
			max *= 1.0 - 0.75 * Math.clamp(t, 0.0, 1.0);
		}
		return Math.clamp(max, MIN_HEIGHT * 2, switchHeight() * 0.8);
	}

	/** Ground distance of the top screen edge from the player, per block of camera distance. */
	private static double groundReach(double tiltDegrees, double halfFov) {
		double pitch = Math.toRadians(90.0 - tiltDegrees);
		double topEdge = pitch - Math.toRadians(halfFov);
		return Math.max(0.5, Math.sin(pitch) / Math.tan(topEdge) - Math.cos(pitch));
	}

	/** True when the camera is tilted so far that the horizon shows; then normal fog stays on. */
	public static boolean horizonVisible() {
		double halfFov = Minecraft.getInstance().options.fov().get() / 2.0;
		return pitch() - halfFov < MIN_TOP_EDGE_ANGLE;
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
		tiltTarget = tiltTarget > 0 ? 0 : lastTilt;
	}

	/**
	 * Steepest tilt allowed at this distance. Zooming out straightens the camera step by step, so
	 * it reaches straight down exactly where the 2D map starts; zooming back in brings the chosen
	 * tilt back.
	 */
	private static double tiltLimit(double distance) {
		double flatStart = switchHeight() * 0.8;
		double fade = MAX_TILT * Math.sqrt(Math.clamp(1.0 - distance / flatStart, 0.0, 1.0));
		return Math.min(maxTiltFor(distance), fade);
	}

	/** Steepest tilt where this distance still stays inside the loaded world. */
	private static double maxTiltFor(double distance) {
		double low = MIN_TILT;
		double high = MAX_TILT;
		if (tiltedMaxDistance(high) >= distance) {
			return high;
		}
		for (int i = 0; i < 20; i++) {
			double mid = (low + high) / 2;
			if (tiltedMaxDistance(mid) >= distance) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return low;
	}

	/** Right mouse button dragged up/down: tilts the camera between MIN_TILT and MAX_TILT. */
	public static void addTilt(double degrees) {
		if (returning || flatOnly) {
			return;
		}
		// only in the tilted view (middle click)
		if (tiltTarget <= 0) {
			return;
		}
		// tilting never zooms: at this distance the tilt stops where it would look past the world
		double limit = tiltLimit(Math.max(targetHeight, height));
		tiltTarget = Math.clamp(tiltTarget + degrees, MIN_TILT, Math.max(MIN_TILT, limit));
		lastTilt = tiltTarget;
	}

	public static double pitch() {
		return 90.0 - tilt * (1.0 - flatAlpha());
	}

	/** Called every frame with the player's eye position while the 3D map view is shown. */
	public static void setAnchor(Vec3 eye) {
		anchor = eye;
	}

	/**
	 * Minecraft builds and picks the chunk sections to draw around the camera, and only up to the
	 * render distance from it, also in height. Our camera hangs far above (and behind) the player,
	 * so lower terrain fell out of that range and left holes. While the 3D map is shown, sections
	 * are chosen around the player instead; what is actually in view still comes from the camera.
	 */
	public static Vec3 sectionCenter(Vec3 cameraPos) {
		return active && !flatOnly && anchor != null ? anchor : cameraPos;
	}

	public static BlockPos sectionCenter(BlockPos cameraPos) {
		return active && !flatOnly && anchor != null ? BlockPos.containing(anchor) : cameraPos;
	}

	/** The point to choose chunk sections around (the player), or null while the normal view is used. Used for Sodium. */
	public static Vec3 cullAnchor() {
		return active && !flatOnly ? anchor : null;
	}

	/**
	 * The far-away, tilted camera confuses vanilla's cave culling: it hides chunk sections it
	 * can't "see" through from the camera, which leaves holes along chunk borders. While the map
	 * is open, culling is switched off and afterwards set back to what it was.
	 */
	public static void updateCulling() {
		Minecraft mc = Minecraft.getInstance();
		boolean wanted = active && !flatOnly;
		if (wanted && !cullingOff) {
			previousSmartCull = mc.smartCull;
			mc.smartCull = false;
			cullingOff = true;
		} else if (!wanted && cullingOff) {
			mc.smartCull = previousSmartCull;
			cullingOff = false;
		}
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
		// zooming out straightens the camera by itself, zooming in brings the chosen tilt back
		double goal = tiltTarget > 0 ? Math.min(tiltTarget, tiltLimit(height)) : 0;
		tilt += (goal - tilt) * step;
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
