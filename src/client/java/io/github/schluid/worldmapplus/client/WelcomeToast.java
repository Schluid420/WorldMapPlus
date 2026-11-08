package io.github.schluid.worldmapplus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Small note on the very first start: the mod is free and where the official download is.
 * Waits for the title screen so it isn't hidden behind the loading screen.
 */
public final class WelcomeToast {
	private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId(10001L);

	private WelcomeToast() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (MapConfig.welcomeShown || minecraft.gui.overlay() != null
					|| !(minecraft.gui.screen() instanceof TitleScreen)) {
				return;
			}
			SystemToast.add(minecraft.gui.toastManager(), ID, Component.translatable("worldmapplus.welcome.title"),
					Component.translatable("worldmapplus.welcome.message"));
			MapConfig.welcomeShown = true;
			MapConfig.save();
		});
	}
}
