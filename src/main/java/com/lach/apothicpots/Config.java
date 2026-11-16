package com.lach.apothicpots;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SPEED;
    public static final ModConfigSpec.IntValue MAX_ROLLS;
    public static final ModConfigSpec.IntValue MIN_TICKS;
    public static final ModConfigSpec.BooleanValue PLAYER_KILL;
    public static final ModConfigSpec.BooleanValue DROP_XP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SPEED = b.comment("Speed multiplier. Cycle time = average spawner delay / speed. 1.0 = same pace as the placed spawner.")
                .defineInRange("speedMultiplier", 1.0, 0.01, 100.0);
        MAX_ROLLS = b.comment("Max loot table rolls per cycle (spawn_count x (1 + echoing), capped here).")
                .defineInRange("maxRollsPerCycle", 8, 1, 256);
        MIN_TICKS = b.comment("Shortest allowed cycle in ticks, before Botany Pots growth modifiers.")
                .defineInRange("minCycleTicks", 20, 1, 72000);
        PLAYER_KILL = b.comment("Kill as a fake player so player-only drops (e.g. wither skeleton skulls) can roll.")
                .define("playerKillDrops", true);
        DROP_XP = b.comment("Drop the mob's XP orbs at the pot each cycle.")
                .define("dropXp", false);
        SPEC = b.build();
    }

    private Config() {}
}
