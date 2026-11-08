# WorldMapPlus

A Fabric mod for Minecraft Java 26.3 that turns the map into a live view of your world. Press M and the camera flies up from your player and shows the real world from above, with mobs, water and the time of day. Keep zooming out and the view blends seamlessly into a 2D map of everything you have explored. It's part of a bigger project of mine, a set of mods that let every player adjust progression and experience to their own taste.

The mod only runs on your side. It works on any server, the server doesn't need it.

## Features

### Live 3D view

| Control | What it does |
| --- | --- |
| M | Opens and closes the map (can be changed in the controls) |
| Mouse wheel | Flies the camera up and down |
| Left mouse button + drag | Moves the view |
| Right mouse button + drag | Turns the view around your player. Let go while moving and it keeps turning for a moment |
| Middle mouse button | Switches between straight down and a tilted bird's-eye view |
| Space | Back to your player, north up |
| Esc | Closes the map |

Clouds and fog are hidden in the map view, so the ground stays clear even from far up. Hotbar and crosshair are hidden too. In singleplayer the game pauses while the map is open, without a pause menu. A red arrow shows where you are and where you look once the camera is high enough.

### 2D map

Above the highest 3D height (358 blocks by default, a bit earlier in the tilted view) the view blends into the 2D map at the same scale, so there is no jump. From there you can zoom out up to 16 times further. The 2D map is drawn from the chunks around you while you play and follows every change in real time, with shaded slopes like the vanilla map and deeper water drawn darker.

### Caves and the Nether

Underground and in the Nether the map opens directly as a 2D map of your own level: tunnels and floors in their real colors, water and lava, and solid rock in dark grey. Every 8 blocks of height have their own cave map, so caves above and below each other don't get mixed up. The End works like the Overworld.

### Discovery mode

Optional: the 2D map only shows the area around the places you have been, everything else stays black. The map is still loaded as usual in the background, so switching the mode off shows everything again. Your trail is always recorded, also while the mode is off.

### Death points

Optional: a skull on the 2D map shows where you died, separately for every dimension. You choose how many of the last death points are shown.

### Saving

The map is saved per world (singleplayer) or per server address (multiplayer) and per dimension in `.minecraft/worldmapplus/`, every 15 seconds and when you leave. The files are small palette PNGs. Only the parts of the map you are looking at are kept in memory, so even big explored worlds stay light on your graphics card.

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) you get a settings screen with four pages. You can also edit `config/worldmapplus.properties` yourself.

| Page | Settings |
| --- | --- |
| Camera | Max. 3D height, switch in tilted view, start height, zoom speed, flight speed, turn sensitivity, turn momentum |
| Display | Pause game, hide HUD, hide clouds, arrow size, arrow from height, arrow color |
| 2D map | 2D map on/off, 2D map only, map speed, height shading, death points, death points shown |
| Discovery mode | On/off, uncover radius |

Hover a setting for an explanation and its default value. "Defaults" on the main page resets everything.

On the very first start a small note in the corner says that the mod is free and where the official download is. Mod Menu also shows buttons to the official pages.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings screen

## Multiplayer

WorldMapPlus is client-side only. Install it on your own game, the server doesn't need it. The 3D view can only show chunks your game has loaded, and the game doesn't pause on servers.

## Translations

Currently English and German. You can add another language with your own resource pack (`assets/worldmapplus/lang/<code>.json`, based on `en_us.json`).

If you would like your translation to be part of the mod, open an issue or a pull request on GitHub, and I will add it.

## Modpacks

You may use WorldMapPlus in modpacks. Please link to the official download (CurseForge or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE)
