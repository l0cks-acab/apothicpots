package com.lach.spawnerpots;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.darkhax.botanypots.common.api.context.BlockEntityContext;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.display.types.Display;
import net.darkhax.botanypots.common.api.data.recipes.CacheableRecipe;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;

/** One dynamic Botany Pots crop that accepts any spawner item carrying a mob. */
public class SpawnerCrop extends Crop implements CacheableRecipe {
    public static final SpawnerCrop INSTANCE = new SpawnerCrop();
    public static final MapCodec<SpawnerCrop> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerCrop> STREAM = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<SpawnerCrop> SERIALIZER = new RecipeSerializer<>() {
        @Override public @NotNull MapCodec<SpawnerCrop> codec() { return CODEC; }
        @Override public @NotNull StreamCodec<RegistryFriendlyByteBuf, SpawnerCrop> streamCodec() { return STREAM; }
    };
    public static final TagKey<Item> SOILS = TagKey.create(Registries.ITEM, SpawnerPots.id("spawner_soils"));

    private final Map<CompoundTag, List<Display>> displays = new ConcurrentHashMap<>();

    private SpawnerCrop() {}

    @Override
    public boolean matches(@NotNull BotanyPotContext input, @NotNull Level level) {
        return SpawnerInfo.of(input.getSeedItem()) != null;
    }

    @Override
    public boolean couldMatch(ItemStack candidate, BotanyPotContext context, Level level) {
        return SpawnerInfo.of(candidate) != null;
    }

    @Override public boolean canBeCached() { return true; }

    /** Cache key is the plain item; the data check happens in couldMatch. */
    @Override public boolean isCacheKey(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.SPAWNER); }

    @Override
    public int getRequiredGrowthTicks(BotanyPotContext context, Level level) {
        SpawnerInfo info = SpawnerInfo.of(context.getSeedItem());
        return info == null ? 1200 : info.cycleTicks();
    }

    @Override
    public boolean isGrowthSustained(BotanyPotContext context, Level level) {
        SpawnerInfo info = SpawnerInfo.of(context.getSeedItem());
        if (info == null || !context.getSoilItem().is(SOILS)) return false;
        if (info.redstone() && context instanceof BlockEntityContext be) {
            return level.hasNeighborSignal(be.pot().getBlockPos());
        }
        return true;
    }

    @Override
    public void onHarvest(BotanyPotContext context, Level level, Consumer<ItemStack> drops) {
        if (!(level instanceof ServerLevel server)) return;
        SpawnerInfo info = SpawnerInfo.of(context.getSeedItem());
        if (info == null) return;
        Vec3 origin = context instanceof BlockEntityContext be ? Vec3.atCenterOf(be.pot().getBlockPos()) : Vec3.ZERO;
        Entity entity = EntityType.loadEntityRecursive(info.entityTag().copy(), server, e -> e);
        if (!(entity instanceof LivingEntity living)) {
            if (entity != null) entity.discard();
            return;
        }
        try {
            living.setPos(origin);
            if (info.burning() && !living.fireImmune()) living.setRemainingFireTicks(200);
            FakePlayer killer = Config.PLAYER_KILL.get() ? FakePlayerFactory.getMinecraft(server) : null;
            DamageSource source = killer != null ? server.damageSources().playerAttack(killer) : server.damageSources().generic();
            LootTable table = server.getServer().reloadableRegistries().getLootTable(living.getLootTable());
            int rolls = info.rollsPerCycle();
            int xp = 0;
            for (int i = 0; i < rolls; i++) {
                LootParams.Builder params = new LootParams.Builder(server)
                        .withParameter(LootContextParams.THIS_ENTITY, living)
                        .withParameter(LootContextParams.ORIGIN, origin)
                        .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                        .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                        .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer);
                if (killer != null) params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).withLuck(killer.getLuck());
                table.getRandomItems(params.create(LootContextParamSets.ENTITY), drops);
                if (Config.DROP_XP.get() && living instanceof Mob) xp += living.getExperienceReward(server, killer);
            }
            if (xp > 0 && context instanceof BlockEntityContext be) {
                BlockPos pos = be.pot().getBlockPos();
                ExperienceOrb.award(server, Vec3.atCenterOf(pos.above()), xp);
            }
        } finally {
            living.discard();
        }
    }

    @Override
    public List<Display> getDisplayState(BotanyPotContext context, Level level) {
        SpawnerInfo info = SpawnerInfo.of(context.getSeedItem());
        if (info == null) return List.of();
        if (displays.size() > 256) displays.clear();
        return displays.computeIfAbsent(info.entityTag(), tag -> List.of(new SpawnerDisplay(tag.copy())));
    }

    @Override
    public void hoverTooltip(ItemStack stack, BotanyPotContext context, Level level, Consumer<Component> lines) {
        SpawnerInfo info = SpawnerInfo.of(stack);
        if (info == null) return;
        float tickrate = level.tickRateManager().tickrate();
        int ticks = stack == context.getSeedItem() && context.getRequiredGrowthTicks() > 0 ? context.getRequiredGrowthTicks() : info.cycleTicks();
        lines.accept(Component.translatable("tooltip.spawnerpots.mob", info.type().getDescription()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.spawnerpots.cycle", StringUtil.formatTickDuration(ticks, tickrate)).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.spawnerpots.rolls", info.rollsPerCycle()).withStyle(ChatFormatting.GRAY));
        if (info.burning()) lines.accept(Component.translatable("tooltip.spawnerpots.burning").withStyle(ChatFormatting.GOLD));
        if (info.redstone()) lines.accept(Component.translatable("tooltip.spawnerpots.redstone").withStyle(ChatFormatting.RED));
        if (!context.getSoilItem().isEmpty() && !context.getSoilItem().is(SOILS)) {
            lines.accept(Component.translatable("tooltip.spawnerpots.wrong_soil").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
