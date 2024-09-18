# CivModern

This is a fork of CivModern specifically for [CivMC](https://civmc.net) on 1.21.1.

## Features

- Compacted items will have a configurably-coloured stack count.

- Ice-road macro (bound to `BACKSPACE` by default).

- Auto-attack macro (bound to `0` by default).

- Hold-key macro for the left mouse button (bound to `-` by default).

- Hold-key macro for the right mouse button (bound to `=` by default).

You may open CivModern's configuration screen in-game with the `R` binding. Or you can modify the config file directly,
which is located at `.minecraft/config/civmodern.json`.

## Added

- Hold W/forward macro (bound to `[` by default).

- Better toggle-sneak (bound to `]` by default). Will automatically deactivate itself if you manually `SHIFT` sneak, or
  if you enter water, or start swimming, sprinting, elytra gliding, or creative flying.

- Added option that shows an item's base-repair level either always, only in advanced tooltips, or never. Set to always
  by default.

- Added option that shows an item's damage level either always, only in advanced tooltips, or never. Set to always by
  default.

## Changed

- Switched the config to YACL. This is a breaking change: your previous configs will no longer work.

## Removed

- Radar has been removed. Why? Because it's duplicated effort. [CombatRadar](https://modrinth.com/mod/combatradar) and
  [CivVoxelMap](https://github.com/Protonull/CivVoxelMap) both provide radars that are legal on CivMC.

## Requirements

- Fabric Loader: `0.16.4` (or newer)
- [Fabric API](https://modrinth.com/mod/fabric-api): `0.103.0+1.21.1` (or newer)
- [YetAnotherConfigLibrary](https://modrinth.com/mod/yacl): `3.5.0+1.21-fabric` (or newer)

## Recommendations

- [ModMenu](https://modrinth.com/mod/modmenu): `11.0.2` (or newer)
