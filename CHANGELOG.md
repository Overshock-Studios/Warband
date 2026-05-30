# Changelog

## Unreleased

- Changed sun-shelter behavior to be predictive: sun-sensitive undead now path to shade at dawn based on daytime + sky exposure + helmet check, instead of waiting until they catch fire. Still gated by `seekShelterEnabled`.
- Changed witch support to pre-buff with real thrown potions: the witch now lobs a Strength splash potion at a frontline squadmate before contact (or Regeneration at a wounded ally), instead of silently applying the effect.
- Changed zombie horde encirclement to distribute squad members around the target by squad index, so zombies actually surround instead of stacking on one flank or beelining single-file. Raised goal priority so the encircle preempts vanilla melee approach until in striking range.
- Changed retreat eligibility to a positive whitelist: only Raiders (illagers), Piglins, and Drowned retreat when wounded or outmatched. Skeletons, creepers, blazes, witches, endermen, phantoms, etc. now fight to the death.
- Added pre-emptive retreat: intelligent mobs pull back at 60% HP when no squadmates are in support range, instead of waiting for the 35% wounded threshold.
- Added phantom sleep-watch: with no visible target, phantoms drift toward the nearest sleeping player within 64 blocks, so a bed isn't an automatic safety guarantee.
- Added spider pre-web on approach: spiders now place webs along the approach path when the target is approaching from up to 12 blocks away, instead of only mid-fight.
- Added creeper pre-stage: creepers without a visible target hold a flanking position near the squad's last-known contact, so they're already in ambush position when the player returns.
- Added passive zombie pack formation: out-of-combat zombies drift toward nearby zombies, naturally clustering into small hordes instead of wandering solo.
- Added skeleton dusk-perch: out-of-combat skeletons climb nearby high ground at dusk/night so they engage from elevation when a player wanders into range.
- Added bounty hunter ambush: when a hunter has closed within 12 blocks of the target without line of sight, they hold position and look around for up to 4 seconds instead of sprinting through, creating "they were waiting around the corner" moments.
- Changed squad backup calls to broadcast distress: when a squad calls for backup, idle nearby squads are alerted to investigate, so fights pull in neighbors instead of staying isolated.
- Fixed stale javadocs and config comments referring to nonexistent SCORE/TIME difficulty modes; the per-player score is the input to REGIONAL, not a mode of its own.

## 1.2.1

- Tuned bounty hunters down: less bonus health, less speed, no enchanted armor, and no long Speed effect.
- Improved bounty hunter pursuit: smaller hitbox scale for two-block gaps and wind-charge jumps for vertical targets.
- Improved bounty hunter rewards with stronger vanilla loot: more emeralds, XP bottles, and occasional supplies.
- Fixed faction-seat state: Warmarshal crowns/broken seats persist, broken seats stop creating new heat/grudges, and Illager Invasion Invokers are prioritized as Warmarshals when present.
- Removed Warband's vanilla-difficulty scaling hooks; Easy/Normal/Hard no longer scale Warband difficulty or boss ability damage.

## 1.2.0

- Added Illager War advancements, including mansion entry, faction milestones, bounty, crusade, and Warmarshal kill progress.
- Added modded stronghold support: Illager Invasion forts/labyrinths are faction camps, and The Lost Castle is a faction seat.
- Added new hostile tactics: spider ceiling crawl, elevated-target hops, ranged repositioning, stealth/status-aware detection, bogged poison back-dash, stray jump shots, and more reliable Enderman disrupt/provoke behavior.
- Added optional Overworld depth difficulty for harder deepslate caves, with `/warband difficulty` showing raw/applied depth bonus.
- Added the bounty hunter overhaul: diamond gear, melee/ranged weapon swapping, parkour pursuit, taunts, glowing mark, stalk teleport, one revive, ominous summon cue, and `/warband debug bounty`.
- Added faction-war escalation and feedback: heat states, war/crusade patrols, in-world event cues, better witness rules, and clearer `/warband intel`.
- Changed config presets to `CUSTOM`, `VANILLA_PLUS` (`vanilla+` accepted), and `FANTASY`; Vanilla+ disables the more RPG-facing systems by default.
- Changed REGIONAL spawn protection: `safeRadius` now applies in REGIONAL mode, with a separate `regionalSpawnRampBlocks` ramp and tighter defaults.
- Fixed Warmarshal Illusioner conversion reliability in worlds without Illager Invasion.
- Fixed revenge and bounty patrols not immediately pathing toward the target player.
- Fixed revenge and bounty leader nametags rendering through walls.
- Fixed rare false-death states where normal Warband-stamped mobs could play death animation/audio but keep fighting.
- Fixed faction heat not tracking when bounty hunters were disabled.
- Fixed revenge grudges expiring from missed random spawn rolls instead of real failed spawn attempts.

## 1.1.0

- Split `bossAbilitiesEnabled` into `witherAbilitiesEnabled` and `enderDragonAbilitiesEnabled` so each boss can be toggled independently. Existing `bossAbilitiesEnabled` values are read once as the default for both new keys.
- Auto-disable Warband Ender Dragon abilities when [True Ending](https://modrinth.com/mod/true-ending) is loaded — detects both the Fabric mod (`mr_true_ending`) and the datapack distribution (via the `true_ending` data namespace), and re-checks on `/reload`.
- Added `/warband reload` (op-only) to re-read `config/warband.properties` without restarting the world.
- Reduced vanilla difficulty double-dipping by disabling the vanilla regional floor by default.
- Regional difficulty ramps faster by default:
  - `regionalIncreaseDelaySeconds` 10 → 0
  - `regionalBlendRate` 0.08 → 0.20
  - `regionalAccelerationPerSample` 0.01 → 0.03
