![Apothic Pots](docs/banner.png)

# Apothic Pots

NeoForge 1.21.1 addon for Botany Pots and Apothic Spawners. Put a spawner in a Botany Pot and it grows that mob's loot.

## How it works
- Silk touch a spawner (Apothic Spawners keeps its mob and stats on the item) and put it in a pot's crop slot.
- Mekanism cardboard box works too: right-click a pot with a boxed spawner, or put it in the pot's crop slot, and the spawner comes out of the box into the pot. You get the empty box back. A boxed spawner fed in by a hopper stays boxed and still grows.
- Right-click a spawner pot with an Apothic Spawners upgrade item (sugar, echo shard, campfire, etc.) to upgrade the spawner in it, same as a placed spawner.
- Right-click a spawner pot with a spawn egg to change the mob, like a placed spawner. The spawner keeps its stats and the egg is used up.
- Soil: soul sand or soul soil (item tag `apothicpots:spawner_soils`).
- Each grow cycle is one "spawn": the mob's loot table is rolled as if a player killed it. No looting.
- Hopper pots auto-collect. Botany Pots growth/yield modifiers, pot types and harvest tools apply as normal.
- Put a sword in the pot's tool slot: looting and other kill enchants apply, and the sword wears down like a hoe does.
- XP from each kill is stored in the pot. Open the pot and click the XP bar on the left to take it all at once, or pipe it out as XP fluid (any fluid in `c:experience`, e.g. Mob Grinding Utils, Ender IO, Sophisticated Core).
- A redstone signal pauses a pot. Pots with an Apothic redstone-control spawner get an on/off lever in the pot screen instead; an outside redstone signal flips that lever too (signal on = on, signal off = off).
- Jade and WTHIT show the mob, spawner stats, progress and stored XP. JEI has a Spawner Pot page per mob with its drops, average per kill and grow time.
- A mini spawner cage sits in the pot with the mob spinning inside (custom Botany Pots display `apothicpots:spawner`).

![A spawner in a pot](docs/02-spawner-in-a-pot.png)

## Spawner stats
| Stat | Effect in the pot |
|---|---|
| min/max delay | cycle time = average delay / `speedMultiplier` |
| spawn_count | loot rolls per cycle |
| echoing | extra rolls: spawn_count x (1 + echoing) |
| burning | drops come out cooked |
| redstone_control | pot only runs while powered |
| everything else | no effect |

Rolls per cycle are capped by `maxRollsPerCycle`.

## Config (`config/apothicpots-common.toml`)
- `speedMultiplier` (1.0)
- `maxRollsPerCycle` (8)
- `minCycleTicks` (20)
- `playerKillDrops` (true): fake-player kill so player-only drops can roll
- `xpOutput` (STORE): STORE keeps XP in the pot, ORBS drops orbs, OFF gives none
- `xpCapacity` (10000): most XP points one pot holds
- `mbPerXp` (20): XP fluid per point when piping
- `xpFluid` (""): XP fluid id; empty = first fluid in `c:experience`
- `redstonePauses` (true): a redstone signal pauses a pot
- `dropMultiplier` (1.0): loot roll multiplier for every mob
- `mobMultipliers` ([]): per-mob loot roll multipliers, e.g. `["minecraft:wither_skeleton=0.5"]`

## Data
- Blacklist: entity tag `apothicpots:blacklist` (wither, ender dragon, warden, elder guardian by default). Apothic's `apothic_spawners:blacklisted_from_spawners` is also respected.
- The crop is the recipe `apothicpots:spawner` (`{"type":"apothicpots:spawner_crop"}`). Remove it with a datapack to turn the feature off.

## Requires
Botany Pots 21.1.41+ (with Bookshelf, Prickle) and Apothic Spawners 1.3+ (with Placebo). Mekanism is optional (cardboard box support).

## Performance
Mean server tick time with zombie spawners on stock stats (dedicated server, chunks force-loaded). Benchmark script: `tools/bench.py`, raw numbers: `docs/performance-results.json`.

![Performance](docs/performance.png)

## Listing art
`python3 tools/listing.py <shots> <out> <ascii.png> docs/performance-results.json <block textures>` (font and textures from the vanilla client jar).
