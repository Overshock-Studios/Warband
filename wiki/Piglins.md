# Piglins

Covers all `AbstractPiglin` (Piglin and Piglin Brute).

## Classification

Smart family. Squad-eligible. Marksman role for piglins carrying crossbows, Bruiser otherwise.

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Piglin Social** | diff >=0.35 | Triggers when a squadmate is hurt - the alerted piglin gets Speed I (4 sec) and Strength I (4 sec) to rally to its ally. Also plays a tactical signal effect. |
| **Pressure Unreachable** | diff >=0.35 | Will reposition to keep pressure on perched targets. |

## Why piglins get tactics earlier
Piglin Social fires at the lowest tactic threshold in the mod (0.35) because the social structure is core to vanilla piglin identity. The other ranged-mob tactics (skeleton smoke, blaze hover, witch support) wait until 0.45+, when the difficulty band is more clearly hostile.

## Family rules
Piglins and Piglin Brutes share friendly-fire immunity. Zombified Piglins do **not** count as the same family - they belong with the zombie family.

## Stat buffs
Standard. Piglins keep their vanilla gold-armor behavior and Crossbow accuracy, just with the linear stat scaling on top.
