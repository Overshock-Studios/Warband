# Warband Wiki

A mob AI and spawning overhaul where the world fights back with strategy. This wiki covers exactly what each mob does at each difficulty band.

## Core systems
- [Difficulty](Difficulty) - how the 0.0..1.0 scalar is derived and what it scales
- [Squad System](Squad-System) - roles, leadership, morale, tiered intelligence
- [Universal Behaviors](Universal-Behaviors) - things every stamped smart mob can do: retreat, regroup, investigate, call backup, arrow-miss perception, sun shelter

## Mob families
- [Zombies and Drowned](Zombies-and-Drowned) (includes zombified piglins and husks)
- [Skeletons](Skeletons) (skeleton, stray, wither skeleton)
- [Spiders](Spiders) (spider, cave spider)
- [Creepers](Creepers)
- [Endermen](Endermen)
- [Piglins](Piglins)
- [Hoglins](Hoglins) (hoglin, zoglin)
- [Slimes](Slimes) (slime, magma cube)
- [Blazes and Witches](Blazes-and-Witches)
- [Phantoms](Phantoms)
- [Extended Mobs](Extended-Mobs) - guardian, shulker, ghast, ravager, warden

## Faction war
- [Illager War](Illager-War) - five factions, doctrines, strongholds, grudges, bounty hunters, rivalries

## Bosses and allies
- [Bosses](Bosses) - Wither and Ender Dragon phase abilities
- [Iron Golems and Snow Golems](Iron-Golems)

## Reading the tables
Each mob page lists the tactics that mob can receive, with the difficulty threshold at which the tactic is rolled in. A mob may carry several tactics at once and chooses which to act on per situation. All numbers refer to the normalized difficulty scalar (`0.0 = vanilla calm`, `1.0 = maximum`).
