# Blazes and Witches

Both are smart-family ranged supports. Both default to Marksman role (Witch is flagged as Support by the illager-invasion compat when applicable).

## Blazes

### Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Blaze Hover** | diff >=0.45 | Holds elevation and fires from positions players struggle to reach. Signals tactical effect when initiating. |
| **Pressure Unreachable** | diff >=0.45 | Will reposition to maintain fire on perched players. |

### Notes
Blazes share friendly-fire only with other Blazes (no shared family with other Nether mobs). The Hover goal makes a diff-0.6 Blaze a serious tower-defense problem - they pick a perch above your line and stay there.

## Witches

### Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Witch Support** | diff >=0.45 | Buffs the closest wounded squadmate with Regeneration I (5 sec) and Resistance I (5 sec). Triggers tactical signal. |
| **Pressure Unreachable** | diff >=0.45 | Will reposition to maintain potion line on perched players. |

### Notes
- Witches don't naturally form witch-only squads, but they slot into illager-faction squads as Support and become the priority kill target.
- Vanilla potion-throwing behavior is unchanged - the Support tactic adds buff potions thrown at squadmates instead.
- A witch in an Iron Choir (Ambush) ambush squad with Regen + Resistance on the bruisers is one of the more dangerous combos in the mod.
