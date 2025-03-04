# CivianMod

This is a hard-fork of [CivModern](https://github.com/okx-code/civmodern) made specifically for [CivMC](https://civmc.net)
on 1.21.4.

## Features

- Compacted items will have a configurably-coloured stack count.

- Ice-road macro (bound to `BACKSPACE` by default).

- Auto-attack macro (bound to `0` by default).

- Hold-key macro for the left mouse button (bound to `-` by default).

- Hold-key macro for the right mouse button (bound to `=` by default).

You may open CivianMod's configuration screen in-game with the `R` binding. Or you can modify the config file directly,
which is located at `.minecraft/config/civianmod.json`.

## Added

- Hold W/forward macro (bound to `[` by default).

- Better toggle-sneak (bound to `]` by default). Will automatically deactivate itself if you manually `SHIFT` sneak, or
  if you enter water, or start swimming, sprinting, elytra gliding, or creative flying.

- Added option that shows an item's base-repair level in its tooltip either always, only in advanced tooltips, or never.
  Set to always by default.

- Added option that shows an item's damage level in its tooltip either always, only in advanced tooltips, or never. Set
  to always by default.

- Added option that shows whether an item is an exp ingredient. Enabled by default.

- Added option, similar to SafeOreBreak, that prevents Minecraft from mining if doing so would break your held tool.
  Enabled by default.

## Changed

- Switched the config to YACL. This is a breaking change: your previous configs will no longer work.

## Removed

- Radar has been removed. Why? Because it's duplicated effort. [CombatRadar](https://modrinth.com/mod/combatradar) and
  [CivVoxelMap](https://github.com/Protonull/CivVoxelMap) both provide radars that are legal on CivMC.

## Requirements

- Fabric Loader: `0.16.9` (or newer)
- [Fabric API](https://modrinth.com/mod/fabric-api): `0.113.0+1.21.4` (or newer)
- [YetAnotherConfigLibrary](https://modrinth.com/mod/yacl): `3.6.2+1.21.4-fabric` (or newer)

## Recommendations

- [ModMenu](https://modrinth.com/mod/modmenu): `v13.0.0` (or newer)
