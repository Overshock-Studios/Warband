# Endermen

## Classification

Smart family. Squad-eligible. The vanilla eye-contact aggro rule is respected even when an enderman is part of a squad - it will not adopt a player target through squad-shared sight or arrow-miss perception alone.

## Eye contact, preserved

Other smart mobs in a squad share targets via line of sight. Endermen do not. A squad of zombies who spotted you will not pass that target to a nearby enderman squadmate - the enderman remains neutral until you either look at it, hit it, or it engages a non-player target.

## Arrow provoke

Shooting an enderman with a projectile - even one it would normally dodge - does not just trigger the vanilla random teleport. Instead:

1. The damage is still dodged.
2. The enderman teleports to a spot **3 blocks in front of where you're looking** (with the safe-footing search vanilla provides). If the front spot has no footing, it falls back to 2.5 blocks behind you.
3. Teleport sound plays at both ends so you hear it leave and arrive.
4. The enderman acquires you as target and enters its "creepy" stared-at state.

Toggle: `endermanProvokeEnabled`.

## Tactics

| Tactic | Threshold | Effect |
|--------|-----------|--------|
| **Enderman Disrupt** | diff >=0.55 | When engaged with a target within 18 blocks, places a short-lived block (or its carried block) in front of where the target is looking to block escape, then teleports behind them. ~5-second cooldown. |
| **Pressure Unreachable** | diff >=0.55 | Will teleport to perches the player is using for safety. |

## Friendly fire
Endermen don't share family with anything else - friendly-fire suppression only triggers within a shared squad.
