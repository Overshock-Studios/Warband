# Changelog

## Unreleased

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
