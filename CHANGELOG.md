# Changelog

## 0.6.1
- Apothic Spawners upgrades work on spawners in pots. Right-click the pot with the upgrade item (sugar, fermented spider eye, echo shard, campfire, etc.) just like a placed spawner. Same limits, same items used up, and offhand recipes work too.

## 0.6.0
- Pots with a redstone-control spawner get a lever in the pot screen. Click it to switch the pot on or off.
- An outside redstone signal (lever, wire, etc.) flips the same switch: signal on turns the pot on, signal off turns it off. You can still flip it by hand in between.
- A switched-off pot keeps its spawner cage, with the mob frozen.

## 0.5.2
- A spawner with no mob in a pot now says to use a spawn egg on the pot (pot screen tooltip, Jade and WTHIT).
- A pot paused by redstone keeps showing its spawner cage. The mob inside stops spinning while paused.

## 0.5.1
- Spawn eggs now work on an empty spawner in a pot (one with no mob yet, like a spawner from the creative menu).

## 0.5.0
- XP: each kill's XP is stored in the pot. Open the pot and click the XP bar on the left to collect it all in one go.
- XP can be piped out as XP fluid (any fluid in `c:experience`: Mob Grinding Utils, Ender IO, Sophisticated Core and others). Config can switch to orbs or turn XP off.
- Sword slot: swords go in the pot's tool slot. Looting and other kill enchants apply and the sword wears down over time.
- Redstone signal pauses a pot. Spawners with Apothic redstone control still need power to run.
- Jade and WTHIT: mob, spawner stats, progress and stored XP when you look at a pot.
- JEI: Spawner Pot page for each mob with its drops, average per kill, grow time and XP.
- Config: global drop multiplier and per-mob multipliers for packmakers. `dropXp` is replaced by `xpOutput`.

## 0.4.0
- Right-click a spawner pot with a spawn egg to change the mob, like a placed spawner. The spawner keeps its stats and the egg is used up (not in creative).
- Works on boxed spawners too: the spawner comes out of the box and you get the box back.
- Blacklisted mobs can't be set with an egg.

## 0.3.0
- Mekanism cardboard box support. Right-click a pot with a boxed spawner, or place it in the pot's crop slot, and the spawner is taken out of the box and put in the pot. The empty box goes back to the player.
- A boxed spawner inserted by a hopper stays boxed and still grows.
- Mekanism is optional.

## 0.2.0
- Renamed to Apothic Pots (mod id `apothicpots`). Recipe, tags, config file (`config/apothicpots-common.toml`) and display ids moved to the new namespace.

## 0.1.3
- Mod logo in the mods list.

## 0.1.2
- The spawner itself now sits in the pot: a mini spawner cage on the soil with the mob spinning inside, like a placed spawner.

## 0.1.1
- Tall mobs (enderman, wither skeleton) render bigger in the pot.

## 0.1.0
- First release: spawner crop for Botany Pots, soul sand/soul soil soils, spawner stat support (delay, spawn count, echoing, burning, redstone control), mob display, config and blacklist tag.
