# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-11-13

### Added

- Free camera tilt: in the tilted view, drag up and down with the right mouse button to tilt the camera between 10° and 80°. Middle click switches between straight down and the last tilt.
- Zooming out in the tilted view straightens the camera step by step, so it blends into the 2D map looking straight down. Zooming back in brings the tilt back. The more the camera is tilted, the earlier this starts, so the view never reaches past the loaded world. Near the horizon the normal fog stays on.

### Changed

- Tilting only swings the camera around the player at the same distance, it doesn't zoom any more.

### Removed

- The setting "Switch in tilted view", the zoom limit now follows the tilt by itself.

### Fixed

- Holes in the terrain (mostly on mountains and along chunk borders), especially with a small render distance. Minecraft only drew chunk sections within the render distance of the far-away map camera, also in height. While the map is open, the sections are now chosen around the player, and the cave culling is switched off.
- The camera could go into the ground on mountains in the tilted view. It now always stays above the terrain at its position (leaves are ignored).
- The same holes with Sodium: while the 3D map is open, Sodium now also chooses the chunk sections around the player. Sodium is not required.
- See-through terrain with Sodium after zooming out and turning the camera.

### Known issues

- With Voxy, small holes can appear in the 3D map view at the border between the real chunks and Voxy's far terrain. Normal gameplay is not affected.

## [1.0.0] - 2026-11-08

First release.

### Added

- Live 3D map view: the camera flies up from the player and looks down on the real world. Mouse wheel for height, drag to move, right drag to turn around the player with momentum, middle click for a tilted view, space to go back to the player.
- Seamless change from the 3D view into a 2D map at the same scale, earlier in the tilted view, and up to 16 times further zoom.
- 2D map drawn in real time from the loaded chunks, with height shading and water depth.
- Cave maps: underground and in the Nether the map shows your own level, with a separate map every 8 blocks of height.
- Discovery mode: the 2D map only shows where you have been.
- Death points on the 2D map, per dimension.
- Clouds and fog hidden in the map view, HUD hidden, game paused in singleplayer.
- Red player arrow that appears once the camera is high enough.
- Maps saved per world or server and per dimension as compact palette PNGs. Only the visible part of the map is kept in memory.
- Settings screen via Mod Menu with the pages Camera, Display, 2D map and Discovery mode.
- A short note on the very first start that the mod is free, and links to the official pages in Mod Menu.
- English and German translations.
