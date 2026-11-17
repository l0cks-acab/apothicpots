package com.lach.apothicpots;

import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Takes a spawner out of its Mekanism cardboard box when a player puts it in a pot, and hands the box back. */
@EventBusSubscriber(modid = ApothicPots.MODID)
public final class BoxUnpacker {
    private BoxUnpacker() {}

    /** Right-click a pot with an empty seed slot while holding a boxed spawner. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!MekBox.isBoxedSpawner(held)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof AbstractBotanyPotBlockEntity pot)) return;
        if (!pot.getSeedItem().isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        Player player = event.getEntity();
        pot.setSeed(MekBox.unbox(held));
        ItemStack box = MekBox.emptyBox();
        if (held.getCount() == 1 && !player.getAbilities().instabuild) {
            player.setItemInHand(event.getHand(), box);
        } else {
            if (!player.getAbilities().instabuild) held.shrink(1);
            giveBack(player, box);
        }
    }

    /** Boxed spawner placed in the pot GUI seed slot. Hoppers keep it boxed; it still works there. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) return;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof AbstractBotanyPotBlockEntity)) continue;
            if (slot.getContainerSlot() != AbstractBotanyPotBlockEntity.SEED_SLOT) continue;
            ItemStack stack = slot.getItem();
            if (!MekBox.isBoxedSpawner(stack)) continue;
            slot.set(MekBox.unbox(stack));
            giveBack(player, MekBox.emptyBox());
        }
    }

    private static void giveBack(Player player, ItemStack box) {
        if (box.isEmpty()) return;
        if (!player.getInventory().add(box)) player.drop(box, false);
    }
}
