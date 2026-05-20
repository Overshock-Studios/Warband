# Warband — Handoff

Written for an agent picking this up cold. Read these first, in order:

1. **`CLAUDE.md`** (repo root) — what the mod is, environment, build, conventions.
2. **`docs/PLAN.md`** — the six-phase implementation plan. This handoff says where
   in that plan we are.
3. This file — current state, gotchas, and the next task.

## TL;DR

Warband is a Minecraft Fabric mod (MC 26.1.2): a mob-AI + spawning overhaul where
difficulty scales with player capability and buffs mob **tactics**, not just stats.
Phases 0, 1, 2 (core), and 4 (HUD) are done and build clean. **Next up: Phase 3 —
Squad AI**, the core feature.

## Build & run

- Build: `.\gradlew.bat build` → jar in `build/libs/`. JDK 26 (see `CLAUDE.md`).
- Client playtest: `.\gradlew.bat runClient`.
- Headless verify: `.\gradlew.bat runServer`. The server reads stdin, so you can
  drive it without a client by piping timed commands, e.g.:
  ```bash
  (sleep 95; echo "warband difficulty"; sleep 5; echo "stop") | ./gradlew.bat runServer
  ```
  Note: a console command has no player, so SCORE-mode difficulty resolves to 0
  from the console — full SCORE/HUD/relief testing needs a real client.

## What is done

| Phase | State | Notes |
|---|---|---|
| 0 — Mob attachment | ✅ done | `com.warband.entity`: `MobData`, `Role`, `WarbandAttachments`. |
| 1 — Difficulty plumbing | ✅ done | `DifficultyManager` + `PlayerScore`; SCORE is default; vanilla-difficulty integration; death relief; `/warband difficulty`. |
| 2 — Spawn Director | ⚠️ core only | Stamping + stat buffs + dimension floors done. **Wave/lull pacing (the "AI Director") is NOT done** — see below. |
| 2.5 — The Ward | ❌ not started | Craftable block that floors local difficulty to 0. |
| 3 — Squad AI | ❌ not started | `SquadCoordinator` is still a stub. **This is the next task.** |
| 4 — Difficulty Lens HUD | ✅ done | Pulled forward early to aid testing. Networking + HUD element. |

### Phase 2 — what "core only" means

`SpawnDirector` currently only *reacts* to mobs the vanilla spawner already
produces (via the `Mob#finalizeSpawn` mixin): it stamps them with `MobData` and
applies difficulty-scaled attribute buffs. It does **not** yet do the
L4D-style wave/lull pacing or compose role-based squads — that is Phase 2b and
is best done together with Phase 3.

### Deferred / TODO markers in code

- `PlayerScore` — TODO: fold an advancement-progression term into the score
  (currently gear-only).
- `WarbandConfig.maxSmartMobsPerPlayer` — config exists but is unused until
  Phase 3 (it caps tactical-AI mobs, which don't exist yet).
- `SpawnDirector` javadoc notes the Phase 2b pacing TODO.

## Architecture (one paragraph)

One difficulty scalar drives everything: `DifficultyManager.getDifficulty(...)`
returns `0.0..1.0`. `PlayerScore` is the SCORE-mode input — a per-player score
stored in a persistent attachment that **ratchets up instantly** on gear upgrade
and **decays down slowly** when capability drops (this avoids difficulty
oscillating with the hotbar). Spawned hostile mobs are stamped with their
difficulty in a `MobData` attachment and given attribute buffs. The HUD is fed by
a once-a-second S2C packet because the client cannot compute difficulty itself.

Package layout: see `CLAUDE.md` (kept current there). Added since: `com.warband.net`
(HUD networking), `com.warband.client` (HUD), `com.warband.command`.

## MC 26 API gotchas (these cost time — keep this list)

MC 26 renamed/replaced a lot. Verified against the deobf jar (see `CLAUDE.md` ›
"Inspect MC jar"):

- `ResourceLocation` → **`net.minecraft.resources.Identifier`**. Build with
  `Identifier.fromNamespaceAndPath(ns, path)`.
- `MobSpawnType` → **`EntitySpawnReason`**.
- `CommandSourceStack` has **no `hasPermission(int)`**. Gate commands with
  `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` (levels:
  `LEVEL_ALL/MODERATORS/GAMEMASTERS/ADMINS/OWNERS`).
- `ServerPlayer` has **no `serverLevel()`** — cast `(ServerLevel) player.level()`.
- HUD: **no `HudRenderCallback`**. Implement `HudElement` and register via
  `HudElementRegistry.addLast(id, element)`. The single method is
  `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`; draw text with
  `GuiGraphicsExtractor.text(Font, String, x, y, argbColor)`.
- Entities **cannot** carry data components (those are item-stack-scoped). Use the
  Fabric **Data Attachment API** (`AttachmentRegistry`) — see `WarbandAttachments`.
- Mappings are pre-mapped Mojang names: **no** `mappings` line in `build.gradle`,
  **no** `loom.officialMojangMappings()`. Access widener header is
  `accessWidener v2 official`.

## Known issues / non-issues

- **HUD shows score and difficulty as the same number** — expected, *not a bug*.
  In SCORE mode `difficulty = score × vanilla-difficulty-ceiling`; on Hard the
  ceiling is 1.0 so they match. They diverge on Easy/Normal, in COMBINED mode, or
  in the Nether/End (dimension floors).
- The HUD uses MC 26's new render-state extractor API and has had only light
  runtime testing — if HUD text misbehaves, suspect `DifficultyLensHud` first.
- `/warband mobs` run from the **server console** reports nothing because
  console-spawned debug mobs spawn at the world-spawn blockpos and suffocate.
  Works fine for a real player standing somewhere safe.

## Next task — Phase 3: Squad AI

See `docs/PLAN.md` › "Phase 3" for the full spec. In short:

- Build `com.warband.ai.Squad` (the shared blackboard: target,
  **last-known-position with decay**, morale, members, backup cooldown).
- Flesh out `SquadCoordinator` (currently a stub) — a throttled server-tick
  driver over active squads.
- Add custom `Goal` classes in `com.warband.ai.goal` (retreat / kite / flank /
  break-LOS / call-backup / regroup), injected into stamped mobs' goal selectors.
- Have `SpawnDirector` compose role-based squads (this is also Phase 2b).
- Honor `maxSmartMobsPerPlayer` here.
- Hard rule from `docs/PLAN.md`: **mobs are smart, not omniscient** — they act on
  line of sight and the blackboard's last-known-position, never wallhack targeting.

## Conventions (also in CLAUDE.md)

- Config: hand-written via `Files.writeString` + text block; never
  `Properties.store()`. Every key gets a validating parser and a comment.
- One difficulty scalar — never scatter difficulty math outside `DifficultyManager`.
- Throttle per-tick work; respect performance caps.
- SemVer in `gradle.properties`; currently `0.3.0`.
- Add mod-compat deps as `compileOnly` + presence checks, never hard `depends`.
