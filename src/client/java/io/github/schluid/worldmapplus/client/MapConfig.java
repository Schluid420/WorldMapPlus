package io.github.schluid.worldmapplus.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Settings, stored in config/worldmapplus.properties. */
public final class MapConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("worldmapplus.properties");

	// Camera
	public static int max3dHeight;
	public static int startHeight;
	public static int zoomSpeed;
	public static int flightSpeed;
	public static int rotateSensitivity;
	public static int rotateInertia;
	public static int tiltSwitchPercent;

	// Display
	public static boolean hideHud;
	public static int arrowSize;
	public static int arrowColor; // 0 = black, 1 = red
	public static int arrowHideHeight; // 0 = always visible
	public static boolean pauseGame;
	public static boolean hideClouds;

	// 2D map
	public static boolean flatMap;
	public static boolean only2d;
	public static boolean deathPoints;
	public static int deathPointCount;
	public static int chunksPerTick;
	public static boolean shading;

	// Discovery mode
	public static boolean discovery;
	public static int discoveryRadius; // 0 = same as max3dHeight

	/** Not a setting: the note on the very first start was shown. Not touched by reset(). */
	public static boolean welcomeShown;

	static {
		reset();
	}

	private MapConfig() {
	}

	public static void reset() {
		max3dHeight = 358;
		startHeight = 48;
		zoomSpeed = 5;
		flightSpeed = 5;
		rotateSensitivity = 5;
		rotateInertia = 5;
		tiltSwitchPercent = 60;
		arrowColor = 1;
		arrowHideHeight = 170;
		only2d = false;
		deathPoints = true;
		deathPointCount = 5;
		discovery = false;
		discoveryRadius = 0;
		flatMap = true;
		hideHud = true;
		arrowSize = 100;
		hideClouds = true;
		pauseGame = true;
		chunksPerTick = 6;
		shading = true;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}
		Properties p = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			p.load(reader);
		} catch (IOException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not read config, using defaults", e);
			return;
		}
		max3dHeight = readInt(p, "max3dHeight", max3dHeight, 32, 2048);
		startHeight = readInt(p, "startHeight", startHeight, 8, 512);
		zoomSpeed = readInt(p, "zoomSpeed", zoomSpeed, 1, 10);
		flightSpeed = readInt(p, "flightSpeed", flightSpeed, 1, 10);
		rotateSensitivity = readInt(p, "rotateSensitivity", rotateSensitivity, 1, 10);
		rotateInertia = readInt(p, "rotateInertia", rotateInertia, 0, 10);
		tiltSwitchPercent = readInt(p, "tiltSwitchPercent", tiltSwitchPercent, 40, 100);
		arrowColor = readInt(p, "arrowColor", arrowColor, 0, 1);
		discovery = readBool(p, "discovery", discovery);
		discoveryRadius = readInt(p, "discoveryRadius", discoveryRadius, 0, 1024);
		arrowHideHeight = readInt(p, "arrowHideHeight", arrowHideHeight, 0, 300);
		only2d = readBool(p, "only2d", only2d);
		deathPoints = readBool(p, "deathPoints", deathPoints);
		deathPointCount = readInt(p, "deathPointCount", deathPointCount, 1, 20);
		flatMap = readBool(p, "flatMap", flatMap);
		hideHud = readBool(p, "hideHud", hideHud);
		arrowSize = readInt(p, "arrowSize", arrowSize, 50, 200);
		hideClouds = readBool(p, "hideClouds", hideClouds);
		pauseGame = readBool(p, "pauseGame", pauseGame);
		chunksPerTick = readInt(p, "chunksPerTick", chunksPerTick, 1, 32);
		shading = readBool(p, "shading", shading);
		welcomeShown = readBool(p, "welcomeShown", welcomeShown);
		// rewrite so new options show up in old files
		save();
	}

	private static int readInt(Properties p, String key, int fallback, int min, int max) {
		try {
			String value = p.getProperty(key);
			return value == null ? fallback : Math.clamp(Integer.parseInt(value.trim()), min, max);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static int effectiveDiscoveryRadius() {
		return discoveryRadius > 0 ? discoveryRadius : max3dHeight;
	}

	private static boolean readBool(Properties p, String key, boolean fallback) {
		String value = p.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value.trim());
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("max3dHeight", Integer.toString(max3dHeight));
		p.setProperty("startHeight", Integer.toString(startHeight));
		p.setProperty("zoomSpeed", Integer.toString(zoomSpeed));
		p.setProperty("flightSpeed", Integer.toString(flightSpeed));
		p.setProperty("rotateSensitivity", Integer.toString(rotateSensitivity));
		p.setProperty("rotateInertia", Integer.toString(rotateInertia));
		p.setProperty("tiltSwitchPercent", Integer.toString(tiltSwitchPercent));
		p.setProperty("arrowColor", Integer.toString(arrowColor));
		p.setProperty("discovery", Boolean.toString(discovery));
		p.setProperty("discoveryRadius", Integer.toString(discoveryRadius));
		p.setProperty("arrowHideHeight", Integer.toString(arrowHideHeight));
		p.setProperty("only2d", Boolean.toString(only2d));
		p.setProperty("deathPoints", Boolean.toString(deathPoints));
		p.setProperty("deathPointCount", Integer.toString(deathPointCount));
		p.setProperty("flatMap", Boolean.toString(flatMap));
		p.setProperty("hideHud", Boolean.toString(hideHud));
		p.setProperty("arrowSize", Integer.toString(arrowSize));
		p.setProperty("hideClouds", Boolean.toString(hideClouds));
		p.setProperty("pauseGame", Boolean.toString(pauseGame));
		p.setProperty("chunksPerTick", Integer.toString(chunksPerTick));
		p.setProperty("shading", Boolean.toString(shading));
		p.setProperty("welcomeShown", Boolean.toString(welcomeShown));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				p.store(writer, "WorldMapPlus settings");
			}
		} catch (IOException e) {
			WorldMapPlusClient.LOGGER.warn("WorldMapPlus: could not save config", e);
		}
	}
}
