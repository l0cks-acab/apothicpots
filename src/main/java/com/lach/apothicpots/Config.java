package com.lach.apothicpots;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public enum XpOutput { OFF, STORE, ORBS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SPEED;
    public static final ModConfigSpec.IntValue MAX_ROLLS;
    public static final ModConfigSpec.IntValue MIN_TICKS;
    public static final ModConfigSpec.BooleanValue PLAYER_KILL;
    public static final ModConfigSpec.EnumValue<XpOutput> XP_OUTPUT;
    public static final ModConfigSpec.IntValue XP_CAPACITY;
    public static final ModConfigSpec.IntValue MB_PER_XP;
    public static final ModConfigSpec.ConfigValue<String> XP_FLUID;
    public static final ModConfigSpec.BooleanValue REDSTONE_PAUSE;
    public static final ModConfigSpec.DoubleValue DROP_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_MULTIPLIERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SPEED = b.comment("Speed multiplier. Cycle time = average spawner delay / speed. 1.0 = same pace as the placed spawner.")
                .defineInRange("speedMultiplier", 1.0, 0.01, 100.0);
        MAX_ROLLS = b.comment("Max loot table rolls per cycle (spawn_count x (1 + echoing) x drop multipliers, capped here).")
                .defineInRange("maxRollsPerCycle", 8, 1, 256);
        MIN_TICKS = b.comment("Shortest allowed cycle in ticks, before Botany Pots growth modifiers.")
                .defineInRange("minCycleTicks", 20, 1, 72000);
        PLAYER_KILL = b.comment("Kill as a fake player so player-only drops (e.g. wither skeleton skulls) can roll.")
                .define("playerKillDrops", true);
        XP_OUTPUT = b.comment("Mob XP each cycle. STORE = kept in the pot (collect from the pot screen, or pipe out as XP fluid). ORBS = dropped as orbs. OFF = none.")
                .defineEnum("xpOutput", XpOutput.STORE);
        XP_CAPACITY = b.comment("Most XP points one pot holds. Extra XP is lost.")
                .defineInRange("xpCapacity", 10000, 1, 1_000_000);
        MB_PER_XP = b.comment("Millibuckets of XP fluid per XP point when piping XP out.")
                .defineInRange("mbPerXp", 20, 1, 1000);
        XP_FLUID = b.comment("XP fluid id pipes drain from a pot. Empty = first fluid in the c:experience tag (Mob Grinding Utils, Ender IO, etc). No fluid = no piping.")
                .define("xpFluid", "");
        REDSTONE_PAUSE = b.comment("A redstone signal pauses a pot. Spawners with Apothic redstone control work the other way: they only run while powered.")
                .define("redstonePauses", true);
        DROP_MULTIPLIER = b.comment("Multiplier on loot rolls for every mob. Fractions roll the leftover by chance.")
                .defineInRange("dropMultiplier", 1.0, 0.0, 64.0);
        MOB_MULTIPLIERS = b.comment("Per-mob loot roll multipliers, as \"mod:mob=multiplier\", e.g. [\"minecraft:wither_skeleton=0.5\"].")
                .defineListAllowEmpty("mobMultipliers", List.of(), () -> "minecraft:zombie=1.0", o -> o instanceof String s && s.contains("="));
        SPEC = b.build();
    }

    private static List<? extends String> parsedFrom;
    private static Map<EntityType<?>, Double> parsed = Map.of();

    public static double mobMultiplier(EntityType<?> type) {
        List<? extends String> raw = MOB_MULTIPLIERS.get();
        if (raw != parsedFrom) {
            Map<EntityType<?>, Double> map = new HashMap<>();
            for (String s : raw) {
                String[] kv = s.split("=", 2);
                ResourceLocation id = ResourceLocation.tryParse(kv[0].trim());
                if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) continue;
                try {
                    map.put(BuiltInRegistries.ENTITY_TYPE.get(id), Math.max(0, Double.parseDouble(kv[1].trim())));
                } catch (NumberFormatException ignored) {}
            }
            parsed = map;
            parsedFrom = raw;
        }
        return parsed.getOrDefault(type, 1.0) * DROP_MULTIPLIER.get();
    }

    private Config() {}
}
