package io.github.schluid.worldmapplus.client;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Slider for whole numbers. */
public class IntSlider extends AbstractSliderButton {
	private final String labelKey;
	private final String unitKey;
	private final int min;
	private final int max;
	private final int step;
	private final IntConsumer setter;
	private String zeroKey;

	public IntSlider(int x, int y, int width, String labelKey, String unitKey, int min, int max, int step,
			IntSupplier getter, IntConsumer setter) {
		super(x, y, width, 20, Component.empty(), (getter.getAsInt() - min) / (double) (max - min));
		this.labelKey = labelKey;
		this.unitKey = unitKey;
		this.min = min;
		this.max = max;
		this.step = step;
		this.setter = setter;
		updateMessage();
	}

	private int current() {
		int raw = (int) Math.round(min + value * (max - min));
		return Math.clamp(Math.round(raw / (float) step) * step, min, max);
	}

	public IntSlider zeroText(String key) {
		this.zeroKey = key;
		updateMessage();
		return this;
	}

	@Override
	protected void updateMessage() {
		if (zeroKey != null && current() == 0) {
			setMessage(Component.translatable(labelKey).append(": ").append(Component.translatable(zeroKey)));
			return;
		}
		Component number = unitKey == null ? Component.literal(Integer.toString(current()))
				: Component.translatable(unitKey, current());
		setMessage(Component.translatable(labelKey).append(": ").append(number));
	}

	@Override
	protected void applyValue() {
		setter.accept(current());
	}
}
