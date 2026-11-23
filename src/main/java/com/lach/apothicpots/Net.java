package com.lach.apothicpots;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.WeakHashMap;
import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.block.menu.BotanyPotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/** XP bar on the pot screen: server tells the client how much XP the open pot holds, client asks to collect it. */
public final class Net {
    public record XpSync(BlockPos pos, int xp, int capacity, boolean switchOn) implements CustomPacketPayload {
        public static final Type<XpSync> TYPE = new Type<>(ApothicPots.id("xp_sync"));
        public static final StreamCodec<ByteBuf, XpSync> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, XpSync::pos, ByteBufCodecs.VAR_INT, XpSync::xp, ByteBufCodecs.VAR_INT, XpSync::capacity,
                ByteBufCodecs.BOOL, XpSync::switchOn, XpSync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CollectXp(BlockPos pos) implements CustomPacketPayload {
        public static final Type<CollectXp> TYPE = new Type<>(ApothicPots.id("collect_xp"));
        public static final StreamCodec<ByteBuf, CollectXp> CODEC = BlockPos.STREAM_CODEC.map(CollectXp::new, CollectXp::pos);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ToggleSwitch(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ToggleSwitch> TYPE = new Type<>(ApothicPots.id("toggle_switch"));
        public static final StreamCodec<ByteBuf, ToggleSwitch> CODEC = BlockPos.STREAM_CODEC.map(ToggleSwitch::new, ToggleSwitch::pos);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client copy of the open pot's XP. */
    public static volatile XpSync clientXp = new XpSync(BlockPos.ZERO, 0, 1, false);

    private static final Map<Player, Long> lastSent = new WeakHashMap<>();

    private Net() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(Net::register);
        NeoForge.EVENT_BUS.addListener(Net::onTick);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1").optional();
        reg.playToClient(XpSync.TYPE, XpSync.CODEC, (msg, ctx) -> clientXp = msg);
        reg.playToServer(CollectXp.TYPE, CollectXp.CODEC, Net::collect);
        reg.playToServer(ToggleSwitch.TYPE, ToggleSwitch.CODEC, Net::toggle);
        DropTable.register(reg);
    }

    @Nullable
    private static AbstractBotanyPotBlockEntity openPot(Player player) {
        if (player.containerMenu instanceof BotanyPotMenu menu && menu.getContext() != null) {
            return menu.getContext().pot();
        }
        return null;
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbstractBotanyPotBlockEntity pot = openPot(player);
        if (pot == null) {
            lastSent.remove(player);
            return;
        }
        int xp = PotXp.get(pot);
        boolean on = PotXp.switchOn(pot);
        long key = (pot.getBlockPos().asLong() * 31 + xp) * 2 + (on ? 1 : 0);
        Long prev = lastSent.get(player);
        if (prev != null && prev == key) return;
        lastSent.put(player, key);
        PacketDistributor.sendToPlayer(player, new XpSync(pot.getBlockPos(), xp, Config.XP_CAPACITY.get(), on));
    }

    private static void toggle(ToggleSwitch msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        AbstractBotanyPotBlockEntity pot = openPot(player);
        if (pot == null || !pot.getBlockPos().equals(msg.pos())) return;
        SpawnerInfo info = SpawnerInfo.of(pot.getSeedItem());
        if (info == null || !info.redstone()) return;
        boolean on = !PotXp.switchOn(pot);
        PotXp.setSwitch(pot, on);
        player.level().playSound(null, pot.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, on ? 0.6f : 0.5f);
    }

    private static void collect(CollectXp msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        AbstractBotanyPotBlockEntity pot = openPot(player);
        if (pot == null || !pot.getBlockPos().equals(msg.pos())) return;
        int xp = PotXp.get(pot);
        if (xp <= 0) return;
        PotXp.set(pot, 0);
        player.giveExperiencePoints(xp);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f,
                0.9f + player.getRandom().nextFloat() * 0.3f);
    }
}
