# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
