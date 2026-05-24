# Phantoms

## Classification

Smart family. Squad-eligible. Marksman role (their swoop counts as a ranged attack).

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Phantom Harass** | diff >=0.45 | Coordinates dive timing across the squad - phantoms stagger their swoops so you don't get a clean reload window between them. Plays a search-effect signal. |
| **Pressure Unreachable** | diff >=0.45 | Will swoop into covered positions that would normally be safe from phantoms. |

## Notes
- Phantoms don't share family with other mobs - friendly-fire only with other Phantoms.
- The Harass tactic is most felt with 3+ phantoms in a squad; solo phantoms behave close to vanilla.
- Phantoms burn in sunlight in vanilla; they don't get the shelter goal (not part of `Zombie`/`AbstractSkeleton` checks). They'll burn out as usual.
