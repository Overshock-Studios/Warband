# Zombies and Drowned

Covers Zombie, Drowned, Zombified Piglin, and Husk.

## Classification

- **Zombie, Husk, Zombified Piglin**: simple family. They commit to the fight - no kiting, flanking, or retreat. They follow the squad target, regroup, and call backup.
- **Drowned**: smart family. Full tactical kit (kite, flank, break LOS, retreat).

All four are smart-eligible for squad formation.

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Zombie Horde** | diff >=0.40 | Coordinates a small surround maneuver - members spread to attack from multiple angles instead of conga-lining. |
| **Water Commit** | diff >=0.70 | Zombies in water stay aggressive instead of passively sinking. Long-term water commitment causes controlled drowning damage, accelerating the vanilla drowned conversion. |
| **Leap Unreachable** | diff >=0.80 | Will leap and stack on each other to reach players on perches. |

## Sun shelter
Zombies caught burning in sunlight path to the nearest shade. Husks don't burn so this never triggers for them. Drowned don't burn either.

## Friendly family
All four (Zombie, Drowned, Husk, ZombifiedPiglin) count as the same "zombie family" for friendly-fire and squad-recruitment purposes.

## Stat buffs
Like all stamped mobs, Zombies get small linear stat buffs scaled by difficulty: max health, attack damage, movement speed, knockback resistance. The defaults are deliberately modest - the threat is the AI, not stat inflation.
