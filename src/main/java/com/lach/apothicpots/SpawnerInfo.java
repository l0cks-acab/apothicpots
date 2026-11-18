package com.lach.apothicpots;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/** Read-only view of a spawner item's block entity data (vanilla keys + Apothic Spawners stats). */
public record SpawnerInfo(EntityType<?> type, CompoundTag entityTag, int minDelay, int maxDelay, int spawnCount,
                          boolean redstone, boolean burning, int echoing) {

    public static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, ApothicPots.id("blacklist"));
    public static final TagKey<EntityType<?>> AS_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("apothic_spawners", "blacklisted_from_spawners"));
    private static final String STAT = "apothic_spawners:";

    @Nullable
    public static SpawnerInfo of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag;
        if (stack.is(Items.SPAWNER)) {
            CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data == null) return null;
            tag = data.copyTag();
        } else {
            tag = MekBox.spawnerTag(stack);
            if (tag == null) return null;
        }
        CompoundTag entity = tag.getCompound("SpawnData").getCompound("entity");
        if (!entity.contains("id", Tag.TAG_STRING)) return null;
        ResourceLocation id = ResourceLocation.tryParse(entity.getString("id"));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type.is(BLACKLIST) || type.is(AS_BLACKLIST)) return null;
        CompoundTag stats = tag.getCompound("stats");
        int min = tag.contains("MinSpawnDelay") ? tag.getShort("MinSpawnDelay") : 200;
        int max = tag.contains("MaxSpawnDelay") ? tag.getShort("MaxSpawnDelay") : 800;
        int count = tag.contains("SpawnCount") ? tag.getShort("SpawnCount") : 4;
        return new SpawnerInfo(type, entity, Math.max(1, min), Math.max(1, max), Math.max(1, count),
                stats.getBoolean(STAT + "redstone_control"), stats.getBoolean(STAT + "burning"),
                Math.max(0, stats.getInt(STAT + "echoing")));
    }

    /** A spawner as it comes out of the world: 200-800 delay, 4 per spawn, no stats. */
    public static SpawnerInfo stock(EntityType<?> type) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        return new SpawnerInfo(type, entity, 200, 800, 4, false, false, 0);
    }

    public static int stockCycleTicks() {
        return stock(net.minecraft.world.entity.EntityType.PIG).cycleTicks();
    }

    public int cycleTicks() {
        double avg = (minDelay + Math.max(minDelay, maxDelay)) / 2.0;
        return Math.max(Config.MIN_TICKS.get(), (int) Math.round(avg / Config.SPEED.get()));
    }

    /** Average loot rolls per cycle after multipliers, capped. */
    public double rollsPerCycleExact() {
        double rolls = (double) spawnCount * (1L + echoing) * Config.mobMultiplier(type);
        return Math.min(Config.MAX_ROLLS.get(), rolls);
    }

    /** Whole rolls for one cycle; the fraction rolls by chance. */
    public int rollsPerCycle(net.minecraft.util.RandomSource random) {
        double exact = rollsPerCycleExact();
        int whole = (int) exact;
        return whole + (random.nextDouble() < exact - whole ? 1 : 0);
    }

    public int rollsPerCycle() {
        return (int) Math.round(rollsPerCycleExact());
    }
}
