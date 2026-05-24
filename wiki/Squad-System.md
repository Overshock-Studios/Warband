# Squad System

Warband mobs above difficulty 0.20 can spawn as coordinated squads with internal roles, a shared blackboard, and group AI.

## Roles

| Role | Behavior |
|------|----------|
| **Leader** | Crowned for the first member of a squad at diff >=0.40. Its death triggers a routing window for the rest of the squad (morale collapses, ~8 seconds of running before the squad steadies and re-engages). |
| **Bruiser** | Closes distance, soaks hits, presses the assault. Default role for melee mobs. |
| **Marksman** | Anything with a ranged attack. Kites, breaks line of sight, seeks elevation. |
| **Support** | Heals or buffs squadmates (e.g. witches in illager invasions). Kites with the marksmen. |

`Skirmisher` is preserved in save data for compatibility but no longer assigned procedurally - it collapses into Marksman behavior.

## What a squad shares

A squad is a runtime blackboard, not a persisted thing. It holds:
- **Members list**
- **Visible target** (cached, refreshed every 5 ticks via line-of-sight from any squadmate)
- **Last-known position** of the target (kept ~12 seconds after losing sight)
- **Morale** (drops when leader falls, regenerates while leader is alive)
- **Routing flag** (set when leader dies)
- **Backup cooldown** (30 seconds between reinforcement calls)

## Tiered intelligence

Not every mob deserves the full tactical kit. Warband splits stamped mobs into two families:

### Simple family
- Zombies (not Drowned)
- Spiders
- Slimes and Magma Cubes
- Hoglins and Zoglins

These get the basics only: follow the squad target, regroup near squadmates, call backup if hurt, investigate last-known positions. **No** kite, flank, break-line-of-sight, or retreat. They commit to the fight.

### Smart family
- Drowned
- Skeletons (and Strays)
- Piglins
- Illagers
- Endermen
- Witches
- Blazes
- Phantoms
- Most extended mobs (Guardian, Shulker, etc.)

These get the full kit: kite, flank, break LOS, retreat at low HP, investigate, regroup, call backup, plus their family-specific tactics.

## Squad formation

When a naturally-spawned smart-eligible mob passes the squad-chance roll, Warband either:
- Adds it to a nearby existing squad (within 18 blocks, similar mob family, ~45% chance at diff >=0.35), or
- Starts a fresh squad. At diff >=0.45 the fresh squad spawns 2-5 additional squadmates of the same type to flesh out the formation.

The squad-chance curve ramps from `naturalSquadChanceMin` at diff 0.20 to `naturalSquadChanceMax` at diff 0.70. Defaults: 0.35 to 0.80.

## Cap

To prevent runaway lag, Warband enforces `maxSmartMobsPerPlayer` (default 24) within a 96-block radius of each player. Spawns above this cap fall through to vanilla.
