package io.github.schluid.worldmapplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldMapPlusClient implements ClientModInitializer {
	public static final String MOD_ID = "worldmapplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category
			.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping openMapKey;

	@Override
	public void onInitializeClient() {
		MapConfig.load();
		WelcomeToast.register();
		openMapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.worldmapplus.open_map",
				InputConstants.KEY_M, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MapData.get().tick(client);
			while (openMapKey.consumeClick()) {
				if (client.level != null && client.gui.screen() == null && !MapCamera.isActive()) {
					client.gui.setScreen(new MapViewScreen());
				}
			}
		});
		LOGGER.info("WorldMapPlus loaded.");
	}
}
