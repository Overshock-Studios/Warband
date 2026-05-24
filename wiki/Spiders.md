# Spiders

Covers Spider and Cave Spider.

## Classification

Simple family. They commit, follow the squad, and don't retreat. Smart-eligible for squad formation. Default role: Bruiser (or Marksman if vanilla flags them as ranged - they don't).

## Tactics

### Spider

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Spider Web** | diff >=0.45 | Trails temporary cobweb blocks while pursuing - obstacles you have to navigate around or break. Webs decay after ~12 seconds. |
| **Sticky Path** | diff >=0.65 (or any Skirmisher) | Drops sticky-block obstacles in chokepoints to slow the player's escape route. Decays after ~8 seconds. |

### Cave Spider

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Cave Spider Ambush** | diff >=0.45 | When within 5 blocks of the target, applies Poison for 5 seconds and gives itself Speed for 4 seconds - the classic "drop on you and bolt." |
| **Pressure Unreachable** | diff >=0.45 | Will climb walls and squeeze through tight spots to keep up. |

## Family rules
- Spiders and Cave Spiders share friendly-fire immunity.
- They are simple-family, so no kiting or break-LOS goals - they straight-line at you. The web/sticky tactics provide their indirect pressure.
