package io.github.schluid.worldmapplus.client;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Settings screen with sub pages, opened from Mod Menu. */
public class SettingsScreen extends Screen {
	private static final int WIDTH = 220;
	private static final int ROW = 24;
	private static final int TOP = 40;

	public enum Page {
		MAIN("title"), CAMERA("camera"), DISPLAY("display"), MAP("map"), DISCOVERY("discovery");

		final String key;

		Page(String key) {
			this.key = "worldmapplus.settings." + key;
		}
	}

	private final Screen parent;
	private final Page page;
	private int row;

	public SettingsScreen(Screen parent) {
		this(parent, Page.MAIN);
	}

	public SettingsScreen(Screen parent, Page page) {
		super(Component.translatable(page.key));
		this.parent = parent;
		this.page = page;
	}

	private static Component onOff(String key, boolean value) {
		return Component.translatable(key).append(": ")
				.append(value ? CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN)
						: CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED));
	}

	private int left() {
		return width / 2 - WIDTH / 2;
	}

	private void add(AbstractWidget widget, String key) {
		widget.setTooltip(Tooltip.create(Component.translatable(key + ".tip")));
		addRenderableWidget(widget);
		row++;
	}

	private int y() {
		return TOP + row * ROW;
	}

	private void toggle(String key, boolean value, Consumer<Boolean> setter) {
		add(Button.builder(onOff(key, value), b -> {
			setter.accept(!value);
			rebuildWidgets();
		}).bounds(left(), y(), WIDTH, 20).build(), key);
	}

	private IntSlider slider(String key, String unitKey, int min, int max, int step,
			java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
		IntSlider slider = new IntSlider(left(), y(), WIDTH, key, unitKey, min, max, step, getter, setter);
		add(slider, key);
		return slider;
	}

	private void pageButton(Page target) {
		add(Button.builder(Component.translatable(target.key).append(" ..."),
				b -> minecraft.setScreenAndShow(new SettingsScreen(this, target))).bounds(left(), y(), WIDTH, 20).build(),
				target.key);
	}

	@Override
	protected void init() {
		row = 0;
		switch (page) {
			case MAIN -> {
				pageButton(Page.CAMERA);
				pageButton(Page.DISPLAY);
				pageButton(Page.MAP);
				pageButton(Page.DISCOVERY);
				addRenderableWidget(Button.builder(Component.translatable("worldmapplus.settings.reset"), b -> {
					MapConfig.reset();
					rebuildWidgets();
				}).bounds(left(), y() + 12, WIDTH, 20).build());
			}
			case CAMERA -> {
				slider("worldmapplus.settings.max_3d_height", "worldmapplus.unit.blocks", 64, 1024, 2,
						() -> MapConfig.max3dHeight, v -> MapConfig.max3dHeight = v);
				slider("worldmapplus.settings.start_height", "worldmapplus.unit.blocks", 16, 256, 4,
						() -> MapConfig.startHeight, v -> MapConfig.startHeight = v);
				slider("worldmapplus.settings.zoom_speed", null, 1, 10, 1,
						() -> MapConfig.zoomSpeed, v -> MapConfig.zoomSpeed = v);
				slider("worldmapplus.settings.flight_speed", null, 1, 10, 1,
						() -> MapConfig.flightSpeed, v -> MapConfig.flightSpeed = v);
				slider("worldmapplus.settings.rotate_sensitivity", null, 1, 10, 1,
						() -> MapConfig.rotateSensitivity, v -> MapConfig.rotateSensitivity = v);
				slider("worldmapplus.settings.rotate_inertia", null, 0, 10, 1,
						() -> MapConfig.rotateInertia, v -> MapConfig.rotateInertia = v);
			}
			case DISPLAY -> {
				toggle("worldmapplus.settings.pause_game", MapConfig.pauseGame, v -> MapConfig.pauseGame = v);
				toggle("worldmapplus.settings.hide_hud", MapConfig.hideHud, v -> MapConfig.hideHud = v);
				toggle("worldmapplus.settings.hide_clouds", MapConfig.hideClouds, v -> MapConfig.hideClouds = v);
				slider("worldmapplus.settings.arrow_size", "worldmapplus.unit.percent", 50, 200, 10,
						() -> MapConfig.arrowSize, v -> MapConfig.arrowSize = v);
				slider("worldmapplus.settings.arrow_hide_height", "worldmapplus.unit.blocks", 0, 300, 10,
						() -> MapConfig.arrowHideHeight, v -> MapConfig.arrowHideHeight = v)
						.zeroText("worldmapplus.settings.arrow_hide_height.never");
				add(Button.builder(Component.translatable("worldmapplus.settings.arrow_color").append(": ")
						.append(Component.translatable("worldmapplus.settings.arrow_color." + MapConfig.arrowColor)),
						b -> {
							MapConfig.arrowColor = (MapConfig.arrowColor + 1) % 2;
							rebuildWidgets();
						}).bounds(left(), y(), WIDTH, 20).build(), "worldmapplus.settings.arrow_color");
			}
			case MAP -> {
				toggle("worldmapplus.option.flat_map", MapConfig.flatMap, v -> MapConfig.flatMap = v);
				toggle("worldmapplus.settings.only_2d", MapConfig.only2d, v -> MapConfig.only2d = v);
				slider("worldmapplus.settings.chunks_per_tick", null, 1, 32, 1,
						() -> MapConfig.chunksPerTick, v -> MapConfig.chunksPerTick = v);
				toggle("worldmapplus.settings.shading", MapConfig.shading, v -> MapConfig.shading = v);
				toggle("worldmapplus.settings.death_points", MapConfig.deathPoints, v -> MapConfig.deathPoints = v);
				slider("worldmapplus.settings.death_point_count", null, 1, 20, 1,
						() -> MapConfig.deathPointCount, v -> MapConfig.deathPointCount = v);
			}
			case DISCOVERY -> {
				toggle("worldmapplus.settings.discovery_on", MapConfig.discovery, v -> MapConfig.discovery = v);
				slider("worldmapplus.settings.discovery_radius", "worldmapplus.unit.blocks", 0, 1024, 8,
						() -> MapConfig.discoveryRadius, v -> MapConfig.discoveryRadius = v)
						.zeroText("worldmapplus.settings.discovery_radius.auto");
			}
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(width / 2 - 75, height - 28, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		Component heading = page == Page.MAIN ? title
				: Component.translatable("worldmapplus.settings.title").append(" – ").append(title);
		graphics.centeredText(font, heading, width / 2, 15, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		MapConfig.save();
		minecraft.setScreenAndShow(parent);
	}
}
