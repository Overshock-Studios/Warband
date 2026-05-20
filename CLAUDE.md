# Warband — Claude Context

## What this is
Vanilla mob AI + spawning overhaul. Difficulty scales with distance from world
spawn (or time / player score). Difficulty buffs mob **AI**, not just stats:
role-based squad spawns and tactical retreat / cover / backup. Applies to
standard overworld, Nether, and End mobs.

A pillager nemesis/warchief system was considered and dropped — it only works
atop a full pillager-behavior overhaul (territory, recruitment, faction memory),
which is a separate mod's worth of scope. Out of scope for Warband.

## Env
- MC 26.1.2, Fabric Loader 0.19.2, Fabric API 0.149.0+26.1.2
- Gradle 9.4.1, JDK 26 at `C:\Program Files\Java\jdk-26.0.1`
- Build: `.\gradlew.bat build` → jar in `build/libs/`
- Run: `.\gradlew.bat runClient` / `runServer` (eula at `run/eula.txt`)
- Mod id `warband`, root pkg `com.warband`, mixin pkg `com.warband.mixin`

## MC 26 mapping
- Pre-mapped Mojang names. NO mappings line in build.gradle. NO `loom.officialMojangMappings()`.
- Access widener header: `accessWidener v2 official`. Wired via `loom { accessWidenerPath = ... }`.

## Inspect MC jar
```powershell
$jar = "C:\Git\Warband\.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-*\26.1.2\*.jar"
cd $env:TEMP
& "C:\Program Files\Java\jdk-26.0.1\bin\jar.exe" xf $jar net/minecraft/the/ClassName.class
& "C:\Program Files\Java\jdk-26.0.1\bin\javap.exe" -p net/minecraft/the/ClassName.class
```

## Architecture
- **One difficulty scalar.** `DifficultyManager.getDifficulty(...)` returns
  `0.0..1.0`. Every system (stat buffs, AI tier, squad size, spawn pacing)
  reads that one number. Do not scatter difficulty math.
- **Stamp difficulty on the mob.** Spawned mobs carry their difficulty via the
  Fabric Data Attachment API (`com.warband.entity.WarbandAttachments#MOB_DATA`,
  holding a `MobData` record) — so a mob owns its level and other mods can read
  it. Entities cannot use data components; those are item-stack-scoped.
- **Performance is a first-class constraint.** Custom AI goals tick per-mob
  per-tick; squads add inter-mob comms; group spawns multiply it. Throttle
  expensive scans (LOS/cover) to every N ticks, cap squad size, honor
  `maxSmartMobsPerPlayer`, share a squad blackboard.
- **Scope the AI honestly.** Voxel A* + Goal/Brain system — not a navmesh. Do
  retreat / kite / flank / break-LOS / backup. Avoid a true dynamic cover graph;
  approximate with periodic LOS-block scans.

## Package layout
- `com.warband` — entrypoints (`WarbandMod`, `WarbandClient`)
- `com.warband.entity` — `MobData`, `Role`, `WarbandAttachments`
- `com.warband.config` — `WarbandConfig` (file-backed, `config/warband.properties`)
- `com.warband.difficulty` — `DifficultyManager`, `DifficultyMode`
- `com.warband.ai` — `SquadCoordinator` (stub)
- `com.warband.spawn` — `SpawnDirector` (stub)
- `com.warband.mixin` — mixins (none yet)

## Config style
- `Files.writeString(path, toPropertiesString(), UTF_8)` with a text block.
- NEVER `Properties.store()` (timestamp + encoding issues on Windows).

## Versioning
- SemVer in `gradle.properties` → `mod_version`. Currently `0.1.0` (pre-release).
- MAJOR breaking · MINOR new feature/config · PATCH fix/balance.

## Build notes
- Fabric API is the only runtime dependency. Add mod-compat deps behind
  `compileOnly` + presence checks, never hard `depends`.
