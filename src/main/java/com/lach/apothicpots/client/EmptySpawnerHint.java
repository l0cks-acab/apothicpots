package com.lach.apothicpots.client;

import com.lach.apothicpots.ApothicPots;
import com.lach.apothicpots.SpawnerInfo;
import net.darkhax.botanypots.common.impl.block.menu.BotanyPotScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** In the pot screen, a spawner with no mob says how to set one. */
@EventBusSubscriber(modid = ApothicPots.MODID, value = Dist.CLIENT)
public final class EmptySpawnerHint {
    private EmptySpawnerHint() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(Items.SPAWNER) || SpawnerInfo.of(event.getItemStack()) != null) return;
        if (!(Minecraft.getInstance().screen instanceof BotanyPotScreen)) return;
        event.getToolTip().add(Component.translatable("tooltip.apothicpots.no_mob").withStyle(ChatFormatting.RED));
    }
}
