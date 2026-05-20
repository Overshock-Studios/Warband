# Warband

**A vanilla mob AI and spawning overhaul.** Near world spawn the game stays
vanilla-calm. The further out you go, the deadlier it gets — and "deadlier"
doesn't just mean bigger health bars. Mobs get *smarter*: they spawn in
role-based squads, fight tactically, retreat, take cover, and call for backup.
Pillagers rise through a nemesis-style warchief hierarchy — named rivals that
remember you.

> Early development. The scaffold runs; the systems below are being built.

## Difficulty

One normalized scalar (`0.0` calm → `1.0` max) drives everything. How it's
derived is configurable:

- **Distance** (default) — difficulty rises the further you are from world spawn.
- **Time** — difficulty rises as the world ages.
- **Score** — difficulty rises with a per-player score.
- **Combined** — the highest of the above wins.

## Planned systems

- **Spawn Director** — paced encounters (lulls and waves), not a random trickle.
- **Squad AI** — role-based groups (Bruiser, Skirmisher, Marksman, Support,
  Leader) sharing a squad blackboard; tactical retreat, flanking, backup calls.
- **Nemesis** — named pillager warchiefs with persistent memory, ranks, and
  strengths/weaknesses; they lead the warbands.
- **Difficulty Lens** — a HUD readout so local threat is visible.

## Building

```
./gradlew.bat build      # jar in build/libs/
./gradlew.bat runClient  # or runServer
```

MC 26.1.2 · Fabric Loader 0.19.2 · Fabric API 0.149.0+26.1.2

## License

MIT.
