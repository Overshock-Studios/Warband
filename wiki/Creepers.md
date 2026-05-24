# Creepers

## Classification

Smart family in the squad system, though they don't form natural squads with other creepers. They can be drafted as squadmates when other squads call for backup if nearby and eligible.

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Creeper Stalk** | diff >=0.55 | Stalks the player from off-axis instead of beelining. Approaches from the flank, breaks contact briefly if spotted. |
| **Pressure Unreachable** | diff >=0.55 | Will path through obstacles, climb mob-stacks, or detonate on the perch supporting you. |

## Naturally Charged

At diff >=0.55 a creeper has a small chance to spawn already charged - the chance ramps from 0% at diff 0.55 up to ~30% at diff 1.0. The spawn is signaled by a visual-only lightning bolt at the creeper's position (no fires set, no AoE damage). The creeper then has its full charged-blast radius and damage.

This is the high-difficulty spectacle reward and is gated by all the same global checks - Peaceful disables it, regional pressure has to be high, etc.

## Stat buffs
Standard stamped-mob stat buffs apply: health, speed, knockback resistance. Note that vanilla creepers have no attack-damage attribute, so the damage modifier silently skips them - their threat comes from the explosion, which is unmodified.
