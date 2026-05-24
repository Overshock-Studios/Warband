# Warband

**A mob AI and spawning overhaul where the world fights back with strategy.**

Regions learn the power of players who spend time there, and the world gets deadlier. But "deadlier" here doesn't mean bullet-sponge mobs with inflated stats. It means mobs that **think**.

> Other difficulty mods only make mobs *stronger*. Warband makes them *smarter*.

## Smarter, Not Spongier

Stat buffs in Warband are deliberately small, just enough to keep clever mobs relevant. The real threat is **coordination**:

- **Squads with internal roles.** Mobs spawn as groups: Bruisers that close, Marksmen that kite and break line of sight, Support that buffs, and a Leader whose death breaks the squad's morale.
- **Real tactics.** They flank, break line of sight around terrain, search last-known positions when they lose you, regroup, pressure unreachable players, and call measured reinforcements.
- **Smart awareness.** Mobs act on what they could plausibly know: sight, sound, squadmate reports, and the *cues you leave behind*. A missed arrow that thunks into the wall draws a squad's attention to where the shot came from; a solo smart mob may turn on you outright. If they lose track of you, they search your *last known position* so you can still outplay them.
- **Tiered intelligence.** Simple mobs (basic zombies, spiders, slimes, hoglins) keep a blunt kit: follow the squad, regroup, call backup. Smarter mobs (drowned, skeletons, piglins, illagers, witches, blazes, endermen) get the full tactical kit and retreat when wounded.

Every mob family brings its own trick: spider webs and sticky ground, skeleton smoke screens, creepers that stalk from the flank, zombie hordes that surround, blazes that hold the high air, witches that support the line, piglin morale, hoglin stampedes, phantom harassment, guardian and shulker pressure, ravager charges, warden discipline, and more. At high difficulty, creepers also have a chance to spawn naturally charged. Sun-burning undead caught out at dawn run for the nearest shade instead of standing and burning. If you shoot endermen with an arrow, it reappears right in front of you, mad.

## The Illager War

Illagers are no longer a loose scatter of mobs, they're **factions**. Five of them, each with its own doctrine, banners, named ranks, and rivals:

- **Black Horn** (Hunt) vs. **Pale Axe** (Siege)
- **Red Ledger** (Command) vs. **Ash Banner** (Burn)
- **Iron Choir** (Ambush), a zealot order with no peer, never intercepted by rivals and never intercepting others

- **Strongholds.** Woodland mansions and pillager outposts are faction seats. A mansion is a single faction's capital, defended by an elevated garrison and commanded by one **Warmarshal**, a named apex boss who is the *smartest* illager in the building, directing the fight from behind his line.
- **Grudges.** Wound a notable illager and let it live, and it remembers. It musters its faction and comes back for you, returning to the very place it happened.
- **Bounty hunters.** Anger a faction enough and it puts a price on you: a relentless elite enforcer picks up your trail.
- **Rivalries.** A revenge patrol can be intercepted by a rival faction, a three-way fight you can turn to your advantage.


## Difficulty

One difficulty scalar drives everything, and you choose how it's derived:

- **Regional** (default). Difficulty follows a running memory of player capability in chunks, backed by vanilla regional difficulty. More players in an area raises the threat.
- **Distance.** Classic: the further from world spawn, the harder. Distance is always measured from overworld spawn, including in the Nether and End.

Warband respects your vanilla difficulty setting (Peaceful turns it off entirely; Easy and Normal lower its ceiling), can optionally ease off after death, and treats the Nether and End as inherently harsher.

## The world isn't helpless

Iron golems receive a support upgrade so villages can actually stand against coordinated raiders. Players can also use goat horns to rally golems and disrupt illager squads.

## Bosses, Farms, and Intel

The Wither and Ender Dragon gain Warband phase abilities, warning effects, and late-fight pressure so boss fights are less passive. Boss ability strength scales with your vanilla difficulty setting: Easy hits about 60% as hard, Normal 85%, Hard at full strength, Peaceful disables the abilities entirely.

Obvious mob-farm conditions trigger anti-farm behavior: trapped crowds try to escape, then stop paying loot and XP if the farm remains abusive.

Warband is server-side. Use `/warband difficulty`, `/warband region`, `/warband mobs`, and `/warband intel` to inspect local threat, the regional difficulty map, stamped mobs, and illager faction state. OP debug tools can spawn test mobs, squads, and revenge parties.

## Compatibility

**Required**: Fabric API. Warband is server-side and can be freely added to an existing world.
- **[Illager Invasion](https://modrinth.com/mod/illager-invasion)**: soft, automatic support; its illagers join the faction system.
- **Structure mods**: mansions and outposts are detected by structure *tag*, so any mod's pillager strongholds can opt in.
- **[Ascendant Armory](https://modrinth.com/mod/ascendant-armory)**: supported from the Ascension Armory side. Warband-stamped mobs can influence core drops when both mods are installed.

## Configuration

Everything is tunable in `config/warband.properties`: difficulty mode and curves, config profiles, squad sizes, stat-buff strength, encounter pacing, role visuals/names/cues, anti-farm tiers, XP scaling, boss abilities, extended mob tactics, goat horn behavior, and every illager subsystem.

---

*MIT licensed.*
