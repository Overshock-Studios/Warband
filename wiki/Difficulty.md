# Difficulty

Warband boils everything down to one normalized scalar between `0.0` (vanilla calm) and `1.0` (maximum pressure). Every other system - stat buffs, AI tier, squad odds, tactic gating, boss intensity - reads that one number.

## How the scalar is derived

You pick the mode in `config/warband.properties`:

### Regional (default)
The world keeps a running chunk-grid memory of how strong nearby players are (gear, weapons, optional progression). Spend time somewhere with capable gear and the local difficulty climbs over minutes; leave and it decays.
- More players in an area raises the threat (multiplayer pressure).
- A vanilla regional-difficulty floor is layered in by default.

### Distance
Classic: the further from world spawn, the harder. Measured from the **overworld spawn point**, even in the Nether and End - so a Nether portal isn't a difficulty reset.

## Vanilla difficulty integration

Warband respects your in-game difficulty setting:
- **Peaceful** turns Warband off entirely.
- **Easy** caps the scalar at 0.60.
- **Normal** caps it at 0.85.
- **Hard** is the full 1.00.

The dimension bonus is applied *before* the ceiling, so Easy in the End is meaningfully lighter than Hard in the End.

## Dimension bonuses

Added on top of the local scalar:
- Nether: +0.25
- End: +0.35

These mean even a freshly-portaled player into the Nether faces non-trivial pressure.

## Death relief (optional)

Off by default. If enabled, a real death stamps a relief window during which the player's contribution to local difficulty is reduced. The intent is to keep regional difficulty honest by default - players who get stomped should learn that area is dangerous - but the toggle exists if you want a softer rebound.

## What scales with difficulty

- **Stat buffs** (small): health, damage, speed, knockback resistance.
- **Squad chance**: ramps from `naturalSquadChanceMin` at diff 0.20 to `naturalSquadChanceMax` at diff 0.70.
- **Retreat**: smart mobs retreat when low HP starting at diff 0.35.
- **Tactics**: each mob's situational tactics unlock at specific thresholds - see individual mob pages.
- **Boss abilities**: Warband boss abilities (added on top of vanilla) scale linearly by the vanilla-difficulty multiplier.
