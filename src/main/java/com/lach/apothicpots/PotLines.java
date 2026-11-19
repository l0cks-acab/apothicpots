package com.lach.apothicpots;

import java.util.ArrayList;
import java.util.List;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Look-at tooltip lines for Jade and WTHIT. Server fills a tag, client turns it into lines. */
public final class PotLines {
    private PotLines() {}

    public static void writeServerData(CompoundTag tag, BlockEntity be) {
        if (!(be instanceof BotanyPotBlockEntity pot)) return;
        tag.putInt("apothicpots_xp", PotXp.get(pot));
        int required = pot.getRequiredGrowthTicks();
        tag.putFloat("apothicpots_progress", required > 0 ? Math.min(1f, pot.growthTime() / required) : 0f);
        tag.putInt("apothicpots_cycle", required);
        tag.putBoolean("apothicpots_running", SpawnerCrop.INSTANCE.isGrowthSustained(pot.getRecipeContext(), pot.getLevel()));
    }

    public static List<Component> lines(BlockEntity be, CompoundTag data, float tickrate) {
        List<Component> out = new ArrayList<>();
        if (!(be instanceof BotanyPotBlockEntity pot)) return out;
        SpawnerInfo info = SpawnerInfo.of(pot.getSeedItem());
        int xp = data.getInt("apothicpots_xp");
        if (info != null) {
            out.add(Component.translatable("tooltip.apothicpots.mob", info.type().getDescription()).withStyle(ChatFormatting.GRAY));
            int cycle = data.contains("apothicpots_cycle") ? data.getInt("apothicpots_cycle") : info.cycleTicks();
            out.add(Component.translatable("tooltip.apothicpots.cycle", StringUtil.formatTickDuration(Math.max(1, cycle), tickrate)).withStyle(ChatFormatting.GRAY));
            out.add(Component.translatable("tooltip.apothicpots.rolls", SpawnerCrop.fmt(info.rollsPerCycleExact())).withStyle(ChatFormatting.GRAY));
            if (info.echoing() > 0) out.add(Component.translatable("tooltip.apothicpots.echoing", info.echoing()).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (info.burning()) out.add(Component.translatable("tooltip.apothicpots.burning").withStyle(ChatFormatting.GOLD));
            if (info.redstone()) out.add(Component.translatable("tooltip.apothicpots.redstone").withStyle(ChatFormatting.RED));
            if (data.contains("apothicpots_progress")) {
                if (data.getBoolean("apothicpots_running")) {
                    int pct = Math.round(data.getFloat("apothicpots_progress") * 100);
                    out.add(Component.translatable("tooltip.apothicpots.progress", pct).withStyle(ChatFormatting.GREEN));
                } else {
                    out.add(Component.translatable("tooltip.apothicpots.paused").withStyle(ChatFormatting.RED));
                }
            }
        }
        else if (pot.getSeedItem().is(net.minecraft.world.item.Items.SPAWNER)) {
            out.add(Component.translatable("tooltip.apothicpots.no_mob").withStyle(ChatFormatting.RED));
        }
        if (xp > 0) out.add(Component.translatable("tooltip.apothicpots.xp", xp, XpMath.levels(xp)).withStyle(ChatFormatting.GREEN));
        return out;
    }
}
