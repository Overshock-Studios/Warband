# Changelog

## 1.1.0

- Split `bossAbilitiesEnabled` into `witherAbilitiesEnabled` and `enderDragonAbilitiesEnabled` so each boss can be toggled independently. Existing `bossAbilitiesEnabled` values are read once as the default for both new keys.
- Auto-disable Warband Ender Dragon abilities when [True Ending](https://modrinth.com/mod/true-ending) is loaded — detects both the Fabric mod (`mr_true_ending`) and the datapack distribution (via the `true_ending` data namespace), and re-checks on `/reload`.
- Added `/warband reload` (op-only) to re-read `config/warband.properties` without restarting the world.
- `factorVanillaDifficulty` default flipped to `false`. Vanilla difficulty already scales mob stats on its own and Warband already respects vanilla via the global ceiling; the extra floor was double-dipping and caused sudden jumps when toggling vanilla difficulty.
- Regional difficulty ramps faster by default:
  - `regionalIncreaseDelaySeconds` 10 → 0
  - `regionalBlendRate` 0.08 → 0.20
  - `regionalAccelerationPerSample` 0.01 → 0.03
