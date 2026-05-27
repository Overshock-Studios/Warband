# Changelog

## Unreleased

- Added Illager War advancements, including mansion entry, faction milestones, bounty, crusade, and Warmarshal kill progress.
- Added modded stronghold support: Illager Invasion forts/labyrinths are faction camps, and The Lost Castle is a faction seat.
- Added new hostile tactics: spider ceiling crawl, elevated-target hops, ranged repositioning, stealth/status-aware detection, bogged poison back-dash, stray jump shots, and more reliable Enderman disrupt/provoke behavior.
- Added optional Overworld depth difficulty for harder deepslate caves, with `/warband difficulty` showing raw/applied depth bonus.
- Added the bounty hunter overhaul: diamond gear, melee/ranged weapon swapping, parkour pursuit, taunts, glowing mark, stalk teleport, one revive, ominous summon cue, and `/warband debug bounty`.
- Added faction-war escalation and feedback: heat states, war/crusade patrols, in-world event cues, better witness rules, and clearer `/warband intel`.
- Changed config presets to `CUSTOM`, `VANILLA_PLUS` (`vanilla+` accepted), and `FANTASY`; Vanilla+ disables the more RPG-facing systems by default.
- Changed REGIONAL spawn protection: `safeRadius` now applies in REGIONAL mode, with a separate `regionalSpawnRampBlocks` ramp and tighter defaults.
- Fixed Warmarshal Illusioner conversion reliability in worlds without Illager Invasion.
- Fixed revenge, war, crusade, and bounty patrols not immediately pathing toward the target player.
- Fixed revenge, crusade, and bounty leader nametags rendering through walls.
- Fixed bounty hunters staying stuck on crossbows in melee.
- Fixed rare false-death states where normal Warband-stamped mobs could play death animation/audio but keep fighting.
- Fixed faction heat not tracking when bounty hunters were disabled.
- Fixed revenge grudges expiring from missed random spawn rolls instead of real failed spawn attempts.

## 1.1.0

- Split `bossAbilitiesEnabled` into `witherAbilitiesEnabled` and `enderDragonAbilitiesEnabled` so each boss can be toggled independently. Existing `bossAbilitiesEnabled` values are read once as the default for both new keys.
- Auto-disable Warband Ender Dragon abilities when [True Ending](https://modrinth.com/mod/true-ending) is loaded — detects both the Fabric mod (`mr_true_ending`) and the datapack distribution (via the `true_ending` data namespace), and re-checks on `/reload`.
- Added `/warband reload` (op-only) to re-read `config/warband.properties` without restarting the world.
- `factorVanillaDifficulty` default flipped to `false`. Vanilla difficulty already scales mob stats on its own and Warband already respects vanilla via the global ceiling; the extra floor was double-dipping and caused sudden jumps when toggling vanilla difficulty.
- Regional difficulty ramps faster by default:
  - `regionalIncreaseDelaySeconds` 10 → 0
  - `regionalBlendRate` 0.08 → 0.20
  - `regionalAccelerationPerSample` 0.01 → 0.03
