# Warband

**A mob AI and spawning overhaul where the world fights back with its head, not just its health bar.**

Near world spawn, Minecraft stays the game you know. The further you push out — or the better-geared you get — the deadlier it becomes. But "deadlier" here doesn't mean bullet-sponge mobs with inflated stats. It means mobs that **think**.

> Other difficulty mods make mobs *stronger*. Warband makes them *smarter*.

## Smarter, not spongier

Stat buffs in Warband are deliberately small — just enough to keep clever mobs relevant. The real threat is **coordination**:

- **Squads with roles.** Mobs spawn as groups — Bruisers that close, Skirmishers that kite, Marksmen that seek elevation, Support that heals, and a Leader whose death breaks the squad's morale.
- **Real tactics.** They flank, break line of sight around terrain, fall back to a last-known position when they lose you, regroup, and call measured reinforcements.
- **Smart, not omniscient.** Mobs act on what they could plausibly know — sight, sound, squadmate reports. No x-ray targeting. When you lose them, they search where they *last saw you* — so you can still outplay them.
- **Honest retreat.** Genuinely dangerous mobs pull back when wounded — but on a leash, so they commit instead of fleeing forever. Mindless mobs still charge to the death; cowardice is earned.

Every mob family brings its own trick: spider webs and sticky ground, skeleton smoke screens, creepers that stalk from the flank, zombie hordes that surround, blazes that hold the high air, witches that support the line, hoglin stampedes, phantom harassment, and more.

## A difficulty that makes sense

One difficulty scalar drives everything, and you choose how it's derived:

- **Regional** (default) — difficulty follows a running memory of player capability, learned per-region. Your well-trodden areas grow dangerous; the wilds you never touched stay wild. **More players in an area raises the threat** — a group faces a real warband.
- **Distance** — classic: the further from world spawn, the harder.

Warband always respects your vanilla difficulty setting (Peaceful turns it off entirely; Easy and Normal lower its ceiling), eases off briefly after a death, and treats the Nether and End as inherently harsher.

## The illager war

Illagers are no longer a loose scatter of mobs — they're **factions**. Five of them, each with its own doctrine, banners, named ranks, and rivals.

- **Strongholds.** Woodland mansions and pillager outposts are faction seats. A mansion is a single faction's capital, defended by an elevated garrison and commanded by one **Warmarshal** — a named apex boss who is the *smartest* illager in the building, directing the fight from behind his line.
- **Grudges.** Wound a notable illager and let it live, and it remembers. It musters its faction and comes back for you — returning to the very place it happened.
- **Bounty hunters.** Anger a faction enough and it puts a price on you: a relentless elite enforcer picks up your trail.
- **Rivalries.** A revenge patrol can be intercepted by a rival faction — a three-way fight you can turn to your advantage.

## The world isn't helpless

Iron golems get a support upgrade so villages can actually stand against smarter raiders, and goat horns can rally golems and disrupt illager squads.

## The Difficulty Lens

A compact HUD readout shows your local **Difficulty** and your own **Power**, so the threat around you is always legible — you can see why a fight went the way it did.

## Compatibility

- **Illager Invasion** — soft, automatic support; its illagers join the faction system.
- **Structure mods** — mansions and outposts are detected by structure *tag*, so any mod's pillager strongholds can opt in.
- Add it to an existing world freely. Fabric API is the only dependency.

## Configuration

Everything is tunable in `config/warband.properties` — difficulty mode and curves, squad sizes, stat-buff strength, encounter pacing, every illager subsystem, and the HUD.

---

*MC 26.1.2 · Fabric. MIT licensed.*
