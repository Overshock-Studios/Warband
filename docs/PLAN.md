# Warband — Implementation Plan

Status: pre-implementation. The repo is a scaffold (`DifficultyManager` + `WarbandConfig`
work; `SpawnDirector` and `SquadCoordinator` are empty stubs).

## Identity

The comparison mods (Better Vanilla Mobs, Hostile Mobs Improve Over Time, Enhanced AI)
buff mob **stats and random abilities**. Warband buffs **tactics and coordination**.

> They make mobs stronger. Warband makes them smarter.

Keep this sharp. Stat buffs exist only to keep smarter mobs relevant — they are not the
feature.

## Design rules

- **Smart, not omniscient.** Mobs use perception they could plausibly have — line of
  sight, sound, squadmate reports via the blackboard. No x-ray sight, no wallhack
  targeting. When a mob loses LOS it searches the **last-known-position**, not the
  player's real coords. This is what separates "tactical" from "aimbot," and it keeps
  difficulty *legible* — the player can see why they lost.
- **One difficulty scalar.** `DifficultyManager` returns `0.0..1.0`; every system reads
  that one number. Do not scatter difficulty math.
- **Performance is a first-class constraint.** Throttle LOS/cover scans to every N
  ticks, cache them on the squad blackboard, cap squad size, honor
  `maxSmartMobsPerPlayer`.

## Known technical correction

CLAUDE.md / stub javadoc say to stamp difficulty onto a mob "as a data component."
Data components are **item-stack-scoped** — entities don't have them. Use the Fabric
**Data Attachment API** (`AttachmentRegistry`): persistent, survives save/load, can
sync to clients. Fix the CLAUDE.md wording when Phase 0 starts.

## Compatibility hazard

Applying attribute modifiers to mob health **breaks auto-farms** (a known problem both
Better Vanilla Mobs and Hostile Mobs Improve Over Time warn about). The **Ward**
(Phase 2.5) is the documented fix — players will uninstall over silently-broken farms,
so the Ward is not optional polish.

---

## Phase 0 — Mob difficulty attachment *(foundation)*

Everything reads this; build it once.

- New package `com.warband.entity`:
  - `MobData` — record `(float difficulty, Role role, int squadId)`.
  - `WarbandAttachments` — registers a persistent `AttachmentType<MobData>`.
  - `MobData.get(entity)` / `set(entity, …)` helpers.
- Fix the "data component" wording in CLAUDE.md and the `SpawnDirector` javadoc.

**Done when:** a mob can be queried for its difficulty and the value persists across
world reload.

## Phase 1 — Difficulty plumbing finished

- Add `getDifficulty(level, pos, player)`; wire `SCORE` and `COMBINED` through it.
- `com.warband.difficulty.PlayerScore` — a per-world `SavedData` tracking a per-player
  score from concrete inputs (advancements, deaths, armor/weapon tier).
- Make `SCORE` the default `difficultyMode`.
- **Per-player resolution rule:** a spawn resolves difficulty against the **nearest
  player**. Write it down so SpawnDirector does not guess.
- **Vanilla difficulty integration:**
  - `respectGlobalDifficulty` (default **on**) — Peaceful → Warband fully off;
    Easy/Normal/Hard scale Warband's ceiling.
  - `factorVanillaDifficulty` (default **off**) — when on, vanilla clamped regional
    difficulty feeds `COMBINED` as an extra `max()` term.
- **Death relief:** config key — difficulty eases for a window after a player death.
- `/warband difficulty` command — prints the local scalar + mode breakdown.

**Done when:** all four modes return real numbers; difficulty tracks player capability
and respects the vanilla difficulty setting.

## Phase 2 — Spawn Director

- **Hook:** mixin into `Mob.finalizeSpawn` (clean choke point for every natural spawn);
  fall back to `ServerEntityEvents.ENTITY_LOAD` if the mixin proves fragile. **Spike
  this first** — the spawn hook is the main technical gamble.
- On spawn of a managed mob (explicit allowlist of standard overworld/Nether/End
  hostiles — not "everything"):
  1. Compute difficulty at pos, write the `MobData` attachment.
  2. Apply stat buffs as `AttributeModifier`s scaled by difficulty (health, damage,
     speed, knockback resist) — modifiers, never base-value edits.
- **AI Director pacing:** per-player tick state machine (BUILD-UP → PEAK → RELAX)
  gating a throttled custom spawn pass.
- Per-dimension difficulty floors (Nether/End start elevated) — new config keys.
- Honor `maxSmartMobsPerPlayer` (count managed mobs near each player before spawning).

**Done when:** mobs further out / vs. stronger players hit harder; encounters arrive
in waves.

## Phase 2.5 — The Ward

A craftable block that pins local difficulty to `0` within a radius. Triple duty:
the player-built safe zone, the auto-farm compatibility fix, and a difficulty relief
valve.

- New block + item + block entity in `com.warband.ward`.
- `DifficultyManager` checks for a nearby active Ward and floors difficulty to 0.
- A mob already inside a Ward radius is not enhanced; once calmed it stays calm.

**Done when:** a Ward placed near a mob farm restores vanilla behavior in its radius.

## Phase 3 — Squad AI *(the heart of the mod)*

- `com.warband.ai.Squad` — the blackboard: shared target, **last-known-position (with
  decay)**, morale, member list, backup cooldown.
- `SquadCoordinator` — registry of active squads; throttled server-tick driver;
  disbands empty squads.
- `Role` enum: Bruiser / Skirmisher / Marksman / Support / Leader.
- Custom `Goal` classes in `com.warband.ai.goal`, injected into a mob's `goalSelector`
  when stamped: `RetreatWhenLowGoal`, `KiteGoal`, `FlankGoal`, `BreakLosGoal`,
  `CallBackupGoal`, `RegroupGoal`. Marksman/"sniper" reposition behavior is proven
  (Mob AI Tweaks) — prioritize it.
- LOS/cover scans throttled, cached on the blackboard (shared, not per-mob).
- Leader death → squad morale collapse → retreat.
- SpawnDirector now spawns role-composed squads instead of lone buffed mobs.
- `/warband debug` — force-spawn a squad at a chosen difficulty (build early in Phase 3).

**Done when:** killing the leader breaks the squad; wounding mobs makes them kite and
call backup; mobs search last-known-position, not real coords.

## Phase 4 — Difficulty Lens HUD

- `WarbandClient` HUD overlay — compact local-threat readout (scalar + trend).
- Optional role nameplates on squad members.
- Sync the difficulty value client-side (attachment sync or a lightweight packet —
  decide here).

**Done when:** the player can see how dangerous where they stand is.

---

## Sizing & order

| Phase | Scope | Relative effort |
|---|---|---|
| 0 — Attachment | Small | ½ |
| 1 — Difficulty | Small-Med | 1 |
| 2 — Spawn Director | Med-Large (mixin risk) | 2 |
| 2.5 — Ward | Medium | 1 |
| 3 — Squad AI | Large | 3 |
| 4 — HUD | Medium | 1½ |

Phases 0 and 1 are low-risk and unblock everything — start there. Phase 2's spawn hook
is the technical gamble; spike it before building on it.

## Cross-cutting

- Config grows each phase (score weights, vanilla-difficulty toggles, death relief,
  dimension floors, pacing timings, HUD toggle).
- Mixins: none until Phase 2; keep `warband.mixins.json` minimal.
- Versioning: Phase 1 → `0.2.0`; each feature phase a MINOR bump; `1.0.0` after Phase 4.
