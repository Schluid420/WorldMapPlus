package io.github.schluid.worldmapplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Map view. Wheel: height, left drag: move, right drag: rotate (sideways) and tilt (up/down),
 * middle click: straight down / tilted,
 * space: back to player.
 */
public class MapViewScreen extends Screen {
	private static final Identifier ARROW = Identifier.fromNamespaceAndPath(WorldMapPlusClient.MOD_ID,
			"textures/gui/arrow.png");
	private static final int ARROW_TEXTURE = 32;
	private static final double ARROW_SIZE = 16;
	private static final double ARROW_MIN = 6;
	private static final double ARROW_FULL_HEIGHT = 48;
	private static final int FLAT_BACKGROUND = 0x101418;
	private static final int TILE_LOADS_PER_FRAME = 4;
	private static final double ROTATE_STEP = 0.08;
	private static final double ARROW_FADE = 20;
	private static final Identifier DEATH = Identifier.fromNamespaceAndPath(WorldMapPlusClient.MOD_ID,
			"textures/gui/death.png");
	private static final int DEATH_SIZE = 16;
	private static final double DEATH_MIN = 5;
	private static final int[] ARROW_COLORS = { 0x000000, 0xE01818 };

	private boolean hudWasHidden;
	private boolean panning;
	private boolean rotating;
	private double rotateSpeed;
	private long lastRotateNanos;

	public MapViewScreen() {
		super(Component.translatable("worldmapplus.map.title"));
	}

	@Override
	protected void init() {
		if (!MapCamera.isActive()) {
			MapCamera.open(MapData.get().isCaveMode() || MapConfig.only2d);
			hudWasHidden = minecraft.gui.hud.isHidden();
			if (MapConfig.hideHud && !hudWasHidden) {
				minecraft.gui.hud.toggle();
			}
		}
	}

	@Override
	public void removed() {
		MapCamera.close();
		if (MapConfig.hideHud && !hudWasHidden && minecraft.gui.hud.isHidden()) {
			minecraft.gui.hud.toggle();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return MapConfig.pauseGame;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// keep the world visible
	}

	private static double forwardX() {
		return -Math.sin(Math.toRadians(MapCamera.yaw()));
	}

	private static double forwardZ() {
		return Math.cos(Math.toRadians(MapCamera.yaw()));
	}

	private static double tiltFactor() {
		return Math.sin(Math.toRadians(MapCamera.pitch()));
	}

	private double pixelsPerBlock() {
		Camera camera = minecraft.gameRenderer.mainCamera();
		float fov = camera != null ? camera.getFov() : 0;
		if (fov <= 1 || fov >= 179) {
			fov = minecraft.options.fov().get();
		}
		double distance = MapCamera.currentHeight() + 1.6;
		return height / (2.0 * Math.max(1.0, distance) * Math.tan(Math.toRadians(fov) / 2.0));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		try {
			double ppb = pixelsPerBlock();
			float alpha = MapCamera.flatAlpha();
			if (alpha > 0) {
				drawFlatMap(graphics, ppb, alpha, partialTick);
			}
			drawArrow(graphics, ppb, partialTick);
		} catch (RuntimeException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not draw map view", e);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void drawFlatMap(GuiGraphicsExtractor graphics, double ppb, float alpha, float partialTick) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		// fade the background in late so the 3D world doesn't get darkened
		float soft = alpha * alpha * (3 - 2 * alpha);
		int a = Math.round(soft * 255) << 24;
		int background = Math.round((float) Math.pow(alpha, 4) * 255) << 24;
		graphics.fill(0, 0, width, height, background | (MapConfig.discovery ? 0x000000 : FLAT_BACKGROUND));
		double centerX = player.getPosition(partialTick).x + MapCamera.offsetX();
		double centerZ = player.getPosition(partialTick).z + MapCamera.offsetZ();
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F, height / 2.0F);
		graphics.pose().rotate((float) Math.toRadians(-(MapCamera.yaw() - 180.0)));
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);
		int size = MapTile.SIZE;
		// half the screen diagonal, so turning the map never cuts off a corner
		double reach = Math.sqrt((double) width * width + (double) height * height) / 2.0 / ppb;
		for (MapTile tile : MapData.get().visibleTiles(centerX - reach, centerZ - reach, centerX + reach,
				centerZ + reach, TILE_LOADS_PER_FRAME)) {
			// no rounding here, otherwise the map jitters while zooming
			double x = (tile.tileX * (double) size - centerX) * ppb + width / 2.0;
			double y = (tile.tileZ * (double) size - centerZ) * ppb + height / 2.0;
			graphics.pose().pushMatrix();
			graphics.pose().translate((float) x, (float) y);
			graphics.pose().scale((float) ppb, (float) ppb);
			graphics.blit(RenderPipelines.GUI_TEXTURED, tile.id, 0, 0, 0, 0, size, size, size, size, size, size,
					a | 0xFFFFFF);
			if (MapConfig.discovery) {
				int fog = MapTile.FOG_SIZE;
				graphics.blit(RenderPipelines.GUI_TEXTURED, tile.fogId, 0, 0, 0, 0, size, size, fog, fog, fog, fog,
						a | 0xFFFFFF);
			}
			graphics.pose().popMatrix();
		}
		if (MapConfig.deathPoints) {
			drawDeathPoints(graphics, ppb, centerX, centerZ, a);
		}
		graphics.pose().popMatrix();
	}

	private void drawDeathPoints(GuiGraphicsExtractor graphics, double ppb, double centerX, double centerZ, int alpha) {
		float turn = (float) Math.toRadians(-(MapCamera.yaw() - 180.0));
		double scale = Math.pow(ARROW_FULL_HEIGHT / Math.max(MapCamera.MIN_HEIGHT, MapCamera.currentHeight()), 0.6);
		float size = (float) Math.clamp(DEATH_SIZE * scale, DEATH_MIN, DEATH_SIZE);
		for (int[] p : MapData.get().deathPoints(MapConfig.deathPointCount)) {
			float x = (float) ((p[0] + 0.5 - centerX) * ppb + width / 2.0);
			float y = (float) ((p[2] + 0.5 - centerZ) * ppb + height / 2.0);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().rotate(-turn);
			graphics.pose().scale(size / DEATH_SIZE, size / DEATH_SIZE);
			int half = DEATH_SIZE / 2;
			graphics.blit(RenderPipelines.GUI_TEXTURED, DEATH, -half, -half, 0, 0, DEATH_SIZE, DEATH_SIZE, DEATH_SIZE,
					DEATH_SIZE, alpha | 0xFFFFFF);
			graphics.pose().popMatrix();
		}
	}

	private void drawArrow(GuiGraphicsExtractor graphics, double ppb, float partialTick) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		double fade = MapConfig.arrowHideHeight <= 0 ? 1.0
				: Math.clamp((MapCamera.currentHeight() - MapConfig.arrowHideHeight) / ARROW_FADE, 0.0, 1.0);
		float visible = (float) Math.max(fade, MapCamera.flatAlpha());
		if (visible <= 0.01F) {
			return;
		}
		int color = Math.round(visible * 255) << 24 | ARROW_COLORS[MapConfig.arrowColor];
		double tilt = tiltFactor();
		double fx = forwardX();
		double fz = forwardZ();
		double ox = MapCamera.offsetX();
		double oz = MapCamera.offsetZ();
		double right = ox * -fz + oz * fx;
		double ahead = ox * fx + oz * fz;
		float x = (float) (width / 2.0 - right * ppb);
		float y = (float) (height / 2.0 + ahead * ppb * tilt);
		double scale = Math.pow(ARROW_FULL_HEIGHT / Math.max(MapCamera.MIN_HEIGHT, MapCamera.currentHeight()), 0.6);
		double size = Math.clamp(ARROW_SIZE * scale, ARROW_MIN, ARROW_SIZE) * MapConfig.arrowSize / 100.0;
		float angle = (float) Math.toRadians(player.getViewYRot(partialTick) - MapCamera.yaw());
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		// squash like the ground in tilted view
		graphics.pose().scale(1.0F, (float) tilt);
		graphics.pose().rotate(angle);
		float s = (float) (size / ARROW_TEXTURE);
		graphics.pose().scale(s, s);
		int half = ARROW_TEXTURE / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, -half, -half, 0, 0, ARROW_TEXTURE, ARROW_TEXTURE,
				ARROW_TEXTURE, ARROW_TEXTURE, color);
		graphics.pose().popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_MIDDLE) {
			MapCamera.toggleTilt();
			return true;
		}
		panning = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
		rotating = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
		if (rotating) {
			MapCamera.setSpin(0);
			rotateSpeed = 0;
			lastRotateNanos = System.nanoTime();
		}
		return panning || rotating || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			panning = false;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && rotating) {
			rotating = false;
			// only keep spinning if the mouse was still moving on release
			boolean stillMoving = (System.nanoTime() - lastRotateNanos) / 1.0E9 < 0.08;
			MapCamera.setSpin(stillMoving ? rotateSpeed : 0);
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (panning && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			double ppb = pixelsPerBlock();
			double fx = forwardX();
			double fz = forwardZ();
			double rightBlocks = dragX / ppb;
			double downBlocks = dragY / (ppb * tiltFactor());
			MapCamera.pan(-(rightBlocks * -fz) + downBlocks * fx, -(rightBlocks * fx) + downBlocks * fz);
			return true;
		}
		if (rotating && event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			double degrees = dragX * ROTATE_STEP * MapConfig.rotateSensitivity;
			MapCamera.rotate(degrees);
			// dragging up tilts towards the horizon
			MapCamera.addTilt(-dragY * ROTATE_STEP * MapConfig.rotateSensitivity);
			long now = System.nanoTime();
			double seconds = Math.max(0.001, (now - lastRotateNanos) / 1.0E9);
			lastRotateNanos = now;
			rotateSpeed = rotateSpeed * 0.6 + (degrees / seconds) * 0.4;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			MapCamera.zoom(scrollY);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (WorldMapPlusClient.openMapKey != null && WorldMapPlusClient.openMapKey.matches(event)) {
			onClose();
			return true;
		}
		if (event.key() == InputConstants.KEY_SPACE) {
			MapCamera.resetPan();
			MapCamera.resetRotation();
			return true;
		}
		return super.keyPressed(event);
	}
}
