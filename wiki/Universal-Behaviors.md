# Universal Behaviors

Cross-cutting behaviors any qualifying mob can get, regardless of family.

## Squad-shared

These apply to any in-squad mob (smart-family roles):

### Regroup
A wandering squadmate paths back toward the squad center. Prevents the squad from splintering across a chase.

### Investigate Last Known
After losing sight of the target, the squad walks toward the spot they last saw the player. Keeps pressure on without granting wallhack vision.

### Call Backup
Hurt squad mobs of certain families (zombies, spiders, piglins, hoglins, illagers) can call in one nearby same-family mob to join the squad. Throttled to once per 30 seconds.

### Flank (Bruisers only, smart family)
Bruisers swing around to attack from off-axis instead of all stacking on the centerline.

### Kite + Break LOS (Marksmen and Support)
Ranged mobs back off when the player closes, and duck behind cover to interrupt sightlines.

### Retreat When Low (smart family, diff >=0.35)
Mobs flee at low HP, or while the squad is routing after losing its leader. Leashed: after ~6 seconds of continuous fleeing the mob commits and fights to the death.

## Universal to all stamped mobs

### Sun Shelter (zombies and skeletons)
On fire from sunlight, with sky visible? Stamped undead path to the nearest shaded tile within ~10 blocks. Husks and Wither Skeletons inherit the goal but never trigger it (they don't burn). Toggle: `seekShelterEnabled`.

### Arrow-Miss Perception
When a player's arrow strikes (hits a block or any entity), every squad with a member within 12 blocks of the impact has its last-known position set to the shooter's location - the squad walks over to investigate. Solo smart mobs in range may aggro on the shooter directly, gated by their difficulty (~14% chance at diff 0.2, ~70% at diff 1.0).

Endermen are excluded from solo aggro - they still require eye contact.

### Pressure Unreachable
When the target is on a vertical or hard-to-reach perch, eligible smart mobs leap, climb mob-stacks, or otherwise press the position instead of giving up.

## Squad target sharing

A squad's target is shared via the blackboard, but each member must have **direct line of sight** to adopt it. The squad isn't omniscient - if a member rounds a corner and loses sight, only the still-seeing members keep the target. Endermen always require eye-contact from players regardless of squad sight.

## Friendly fire

Same-squad and same-family allies don't damage each other. If a creeper blasts a zombie next to it, the zombie doesn't aggro on the creeper. Intentional hits (the attacker was specifically targeting the victim) still drop both targets; collateral splash doesn't.
