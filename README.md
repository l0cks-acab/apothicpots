![Spawner Pots](docs/banner.png)

# Spawner Pots

NeoForge 1.21.1 addon for Botany Pots and Apothic Spawners. Put a spawner in a Botany Pot and it grows that mob's loot.

## How it works
- Silk touch a spawner (Apothic Spawners keeps its mob and stats on the item) and put it in a pot's crop slot.
- Soil: soul sand or soul soil (item tag `spawnerpots:spawner_soils`).
- Each grow cycle is one "spawn": the mob's loot table is rolled as if a player killed it. No looting.
- Hopper pots auto-collect. Botany Pots growth/yield modifiers, pot types and harvest tools apply as normal.
- A mini spawner cage sits in the pot with the mob spinning inside (custom Botany Pots display `spawnerpots:spawner`).

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

## Config (`config/spawnerpots-common.toml`)
- `speedMultiplier` (1.0)
- `maxRollsPerCycle` (8)
- `minCycleTicks` (20)
- `playerKillDrops` (true): fake-player kill so player-only drops can roll
- `dropXp` (false): drop the mob's XP at the pot

## Data
- Blacklist: entity tag `spawnerpots:blacklist` (wither, ender dragon, warden, elder guardian by default). Apothic's `apothic_spawners:blacklisted_from_spawners` is also respected.
- The crop is the recipe `spawnerpots:spawner` (`{"type":"spawnerpots:spawner_crop"}`). Remove it with a datapack to turn the feature off.

## Requires
Botany Pots 21.1.41+ (with Bookshelf, Prickle) and Apothic Spawners 1.3+ (with Placebo).

## Performance
Mean server tick time with zombie spawners on stock stats (dedicated server, chunks force-loaded). Benchmark script: `tools/bench.py`, raw numbers: `docs/performance-results.json`.

![Performance](docs/performance.png)

## Listing art
`python3 tools/listing.py <shots> <out> <ascii.png> docs/performance-results.json <block textures>` (font and textures from the vanilla client jar).
