package com.lach.apothicpots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Pot drops per mob for JEI. Loot tables only live on the server, so the server samples each mob's
 * table and sends the averages to clients.
 */
public final class DropTable {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int SAMPLES = 100;
    private static final int MAX_DROPS = 12;

    public record Drop(ItemStack stack, float perKill) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Drop> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Drop::stack, ByteBufCodecs.FLOAT, Drop::perKill, Drop::new);
    }

    /** One JEI page: a mob at stock spawner stats. */
    public record MobDrops(ResourceLocation mob, int cycleTicks, float killsPerCycle, float xpPerKill, List<Drop> drops) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MobDrops> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, MobDrops::mob, ByteBufCodecs.VAR_INT, MobDrops::cycleTicks,
                ByteBufCodecs.FLOAT, MobDrops::killsPerCycle, ByteBufCodecs.FLOAT, MobDrops::xpPerKill,
                Drop.CODEC.apply(ByteBufCodecs.list()), MobDrops::drops, MobDrops::new);

        public EntityType<?> type() { return BuiltInRegistries.ENTITY_TYPE.get(mob); }
    }

    public record Sync(List<MobDrops> mobs) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ApothicPots.id("drop_table"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                MobDrops.CODEC.apply(ByteBufCodecs.list()).map(Sync::new, Sync::mobs);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static List<MobDrops> server;
    /** Latest table received from the server (client side). */
    public static volatile List<MobDrops> client = List.of();
    /** Called on the client when a new table arrives (JEI hooks in here). */
    public static volatile Runnable onClientUpdate = () -> {};

    private DropTable() {}

    static void register(PayloadRegistrar reg) {
        reg.playToClient(Sync.TYPE, Sync.CODEC, (msg, ctx) -> {
            client = msg.mobs();
            onClientUpdate.run();
        });
        NeoForge.EVENT_BUS.addListener(DropTable::onSync);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> server = build(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> server = null);
    }

    private static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null || server == null) server = build(event.getPlayerList().getServer());
        Sync msg = new Sync(server);
        event.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, msg));
    }

    private static List<MobDrops> build(MinecraftServer mc) {
        long start = System.nanoTime();
        ServerLevel level = mc.overworld();
        FakePlayer killer = FakePlayerFactory.getMinecraft(level);
        List<MobDrops> out = new ArrayList<>();
        int cycle = SpawnerInfo.stockCycleTicks();
        for (SpawnEggItem egg : SpawnEggItem.eggs()) {
            EntityType<?> type = egg.getType(new ItemStack(egg));
            if (type.is(SpawnerInfo.BLACKLIST) || type.is(SpawnerInfo.AS_BLACKLIST)) continue;
            Entity entity;
            try {
                entity = type.create(level);
            } catch (Exception e) {
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                if (entity != null) entity.discard();
                continue;
            }
            try {
                Vec3 origin = Vec3.atCenterOf(level.getSharedSpawnPos());
                living.setPos(origin);
                LootTable table = mc.reloadableRegistries().getLootTable(living.getLootTable());
                Map<Item, ItemStack> first = new LinkedHashMap<>();
                Map<Item, Integer> counts = new LinkedHashMap<>();
                int xp = 0;
                for (int i = 0; i < SAMPLES; i++) {
                    LootParams params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, living)
                            .withParameter(LootContextParams.ORIGIN, origin)
                            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                            .create(LootContextParamSets.ENTITY);
                    table.getRandomItems(params, s -> {
                        if (s.isEmpty()) return;
                        first.putIfAbsent(s.getItem(), s.copyWithCount(1));
                        counts.merge(s.getItem(), s.getCount(), Integer::sum);
                    });
                    if (living instanceof Mob) xp += living.getExperienceReward(level, killer);
                }
                List<Drop> drops = new ArrayList<>();
                counts.forEach((item, n) -> drops.add(new Drop(first.get(item), n / (float) SAMPLES)));
                drops.sort(Comparator.comparingDouble(d -> -d.perKill()));
                if (drops.isEmpty()) continue;
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                SpawnerInfo stock = SpawnerInfo.stock(type);
                out.add(new MobDrops(id, cycle, (float) stock.rollsPerCycleExact(), xp / (float) SAMPLES,
                        List.copyOf(drops.subList(0, Math.min(MAX_DROPS, drops.size())))));
            } catch (Exception e) {
                LOG.debug("Skipped {} for pot drop table", type, e);
            } finally {
                living.discard();
            }
        }
        out.sort(Comparator.comparing(m -> m.mob().toString()));
        LOG.info("Sampled pot drops for {} mobs in {} ms", out.size(), (System.nanoTime() - start) / 1_000_000);
        return out;
    }
}
