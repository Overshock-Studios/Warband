# Skeletons

Covers Skeleton, Stray, and Wither Skeleton (all `AbstractSkeleton`).

## Classification

Smart family. Default role is **Marksman** since they're ranged - they kite the player, seek elevation, and break line of sight under pressure. Retreat at low HP from diff >=0.35.

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Pressure Unreachable** | diff >=0.50 | Reposition to maintain firing lines on perched players. |
| **Skeleton Smoke** | diff >=0.60 | Drops a brief smoke effect to break the player's vision and cover a retreat or reposition. |
| **Frost Walker** *(Stray only)* | diff >=0.55 | Strays can freeze the ground beneath them, slowing pursuers and giving themselves a kiting surface. |

## Family rules
- All three subtypes share friendly-fire immunity and squad-recruitment as one family.
- Wither Skeletons inherit the sun-shelter goal but never trigger it (they don't burn).
- Skeletons in dawn light path to shade like other undead.

## Bow play
Vanilla bow accuracy and timing is unchanged - the difference is positioning. A diff-0.6 skeleton squad will set up firing arcs, refuse to bunch into your sword range, and reposition when you start closing.
