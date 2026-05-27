# Changelog

## Unreleased

- Fixed: Warmarshal Illusioner swap only triggered when the mansion's first-loaded illager was an Evoker. Vindicators (more commonly the first to load) silently kept their type. The swap now applies to any vanilla `AbstractIllager` so the Warmarshal is reliably an Illusioner without Illager Invasion.
- Fixed: revenge / war / crusade / bounty spawns set `setTarget(player)` but didn't kick off pathfinding, so the patrol could sit at the spawn position waiting for the player to wander into sight range. New `directVengeancePursuit` helper sets the patrol target (`PatrollingMonster.setPatrolTarget`), kicks the navigation, and applies Speed I — the patrol walks toward the player from the moment it spawns.
- Fixed: revenge / crusade / bounty leader nametags rendered through walls (`setCustomNameVisible(true)`). Nametags are now hover-only — visible when the player aims at the mob, occluded by terrain otherwise.
- Added modded stronghold structure tags: Illager Invasion's `illager_fort` and `labyrinth` now count as faction camps, and The Lost Castle's `tlc:lost_castle` counts as a faction seat.
- Added mansion-entry advancement (`Into the Mansion`) using the vanilla `minecraft:location` trigger with the `mansion` structure predicate — no custom code, just JSON.
- Bounty hunter overhaul — meant to feel like Death from Puss in Boots, not another pillager:
  - **Full diamond kit, fully enchanted** (Protection IV on chest/helm, III on legs/boots, Unbreaking III, sharpness IV diamond sword in offhand, Quick Charge III + Piercing IV crossbow in main). 1% drop chance per piece — rare reward, not farmable.
  - **Tuned stat buffs**: +26 max health (50 HP total), +6 attack damage, +0.5 knockback resistance, +15% movement speed, plus persistent Resistance I / Strength I / Jump Boost II / Speed I. Diamond armor + the one-time revive carry the survivability so the fight isn't a spongey grind.
  - **Hybrid combat**: new `BountyMeleeGoal` takes over at ≤4 blocks (crossbow keeps the ranged engagement at distance, sword for close work).
  - **Pillar-up parkour** (`BountyClimbGoal`) for vertical targets — placements capped at 12.
  - **Taunts** (`BountyTauntGoal`): periodic faction-villain quips on the action bar plus an evoker/illusioner/ravager/witch sound so you hear them coming.
  - **Active pursuit** (`BountyChaseGoal`): re-paths to the player's position every 30 ticks regardless of line of sight, so they actively close instead of waiting at the spawn site until the player wanders into view.
  - **Mark of debt** (`BountyMarkGoal`): refreshes Glowing on the marked player every 5 seconds — mutual visibility, lets the player feel marked and the hunter stay easy to track at a glance.
  - **One revive** (totem-of-undying behavior): bounty hunter survives a single killing blow at 50% HP with a Regen II + Strength I rebound and a totem visual.
  - **Stalker teleport** (`BountyStalkGoal`): if the hunter is kept >48 blocks away for 25 seconds, smoke-puffs to a position three blocks behind the player. Cooldown 45 seconds.
  - **Ominous summon at the player**: when a bounty hunter spawns, the player hears `EVOKER_PREPARE_SUMMON` + a faint raid horn at *their own position* — they know he's coming before they see him.
- Added `/warband debug bounty <difficulty>` to force-spawn a bounty hunter for testing.
- **Illager War advancement chain.** Seven advancements track progress through the faction war: notice → first grudge → first revenge patrol → bounty posted → at war → crusade called → Warmarshal slain (challenge). Backed by a single custom criterion (`warband:event` keyed by `kind` string) so new milestones can be added with JSON only.
- Warmarshal is now a vanilla **Illusioner** in worlds without Illager Invasion installed — Illusioners otherwise never spawn naturally, so the apex enemy reads as distinctly faction-tier instead of "just another evoker". With Illager Invasion installed the swap is skipped; the mod's Invoker (or whichever boss-tier illager it placed) keeps the title.
- Faction event announcements (revenge, war patrol, crusade, bounty hunter, rival interception) are now delivered as in-world cues — distinctive horn/smoke/particle bursts at the muster site plus an action-bar message — instead of cluttering chat. Each event type has its own audio/visual signature (revenge: raid horn + angry villager motes; bounty: ominous goat horn + soul fire; war: raid horn + illusioner cast; crusade: deep horn + wither + thunder + soul). Named-survivor and Warmarshal-death lines remain in chat since they're persistent state changes.
- **Vengeance escalation states.** Each faction's relationship with a player now progresses through `quiet → noticed → watching → hostile → at war → crusade` based on accumulated heat. At WAR (heat ≥ 80) the faction sends unprompted war patrols every ~12 minutes. At CRUSADE (heat ≥ 150) it stages full assaults at the player's respawn point every ~30 minutes, then sheds enough heat to drop back to WAR. State is shown in `/warband intel`.
- Illager faction system feedback overhaul:
  - First kill of any factioned illager always forms a grudge — the system introduces itself rather than staying silent.
  - A crowd remembers: 3+ illagers witnessing a kill drop the notability filter; rank-and-file pillagers will form grudges if there's a group around.
  - Banner-carrying illagers (faction representatives) always count as notable witnesses.
  - Every factioned-illager kill adds a small heat drip (2) to that faction so `/warband intel` reflects the relationship from the start.
  - `/warband intel` now distinguishes `noticed / watching / hostile / bounty incoming` postures and explains the empty state instead of just saying "No faction intel".
- `safeRadius` is now honored in REGIONAL mode (previously only DISTANCE). Inside `safeRadius` blocks of overworld spawn, Warband difficulty scales to 0 regardless of cell pressure; ramps to full at `maxDifficultyRadius`.
- Fixed: faction heat (`/warband intel` "Faction heat" lines) was silently not tracked when `illagerBountyHuntersEnabled=false`. Heat is now always recorded; only bounty-hunter spawning remains gated by the toggle.
- Fixed: an illager revenge grudge could quietly expire after ~9 minutes if the per-scan 35% spawn roll missed three times in a row — random-gate misses were burning the grudge's 3-attempt budget without ever attempting to spawn a patrol. Only real spawn attempts count against the budget now.

## 1.1.0

- Split `bossAbilitiesEnabled` into `witherAbilitiesEnabled` and `enderDragonAbilitiesEnabled` so each boss can be toggled independently. Existing `bossAbilitiesEnabled` values are read once as the default for both new keys.
- Auto-disable Warband Ender Dragon abilities when [True Ending](https://modrinth.com/mod/true-ending) is loaded — detects both the Fabric mod (`mr_true_ending`) and the datapack distribution (via the `true_ending` data namespace), and re-checks on `/reload`.
- Added `/warband reload` (op-only) to re-read `config/warband.properties` without restarting the world.
- `factorVanillaDifficulty` default flipped to `false`. Vanilla difficulty already scales mob stats on its own and Warband already respects vanilla via the global ceiling; the extra floor was double-dipping and caused sudden jumps when toggling vanilla difficulty.
- Regional difficulty ramps faster by default:
  - `regionalIncreaseDelaySeconds` 10 → 0
  - `regionalBlendRate` 0.08 → 0.20
  - `regionalAccelerationPerSample` 0.01 → 0.03
