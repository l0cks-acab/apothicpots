package com.lach.apothicpots;

import dev.shadowsoffire.apothic_spawners.ASObjects;
import dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile;
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Right-click a spawner pot with an Apothic Spawners upgrade item to apply it to the spawner in the pot,
 * the same way as a placed spawner. Uses Apothic's own modifier recipes on a detached spawner tile.
 */
@EventBusSubscriber(modid = ApothicPots.MODID)
public final class UpgradeApply {
    private UpgradeApply() {}

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || held.getItem() instanceof SpawnEggItem) return;
        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof AbstractBotanyPotBlockEntity pot)) return;
        ItemStack seed = pot.getSeedItem();
        boolean boxed = MekBox.isBox(seed);
        CompoundTag tag = spawnerTag(seed);
        if (tag == null) return;

        Player player = event.getEntity();
        ItemStack other = player.getItemInHand(event.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ApothSpawnerTile tile = new ApothSpawnerTile(pot.getBlockPos(), Blocks.SPAWNER.defaultBlockState());
        tile.setLevel(level);
        tile.loadCustomOnly(tag, level.registryAccess());
        SpawnerModifier mod = SpawnerModifier.findMatch(tile, held, other);
        if (mod == null) return;
        if (level.isClientSide) {
            // Server decides; stop the pot's own right-click so the item isn't used twice.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!mod.apply(tile)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        CompoundTag out = tile.saveCustomOnly(level.registryAccess());
        out.putBoolean("modified", true);
        out.remove("x");
        out.remove("y");
        out.remove("z");
        ItemStack spawner = new ItemStack(Items.SPAWNER);
        BlockItem.setBlockEntityData(spawner, BlockEntityType.MOB_SPAWNER, out);
        pot.setSeed(spawner);
        pot.setChanged();
        if (boxed) BoxUnpacker.giveBack(player, MekBox.emptyBox());

        if (!player.isCreative()) {
            held.shrink(1);
            if (mod.consumesOffhand()) other.shrink(1);
        }
        if (player instanceof ServerPlayer sp) ASObjects.MODIFIER_TRIGGER.trigger(sp, tile, mod);
        level.sendBlockUpdated(pot.getBlockPos(), pot.getBlockState(), pot.getBlockState(), 3);
        level.playSound(null, pot.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
    }

    /** Spawner block entity data from a plain or boxed spawner in the seed slot. */
    @Nullable
    private static CompoundTag spawnerTag(ItemStack seed) {
        if (seed.is(Items.SPAWNER)) {
            CustomData data = seed.get(DataComponents.BLOCK_ENTITY_DATA);
            CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
            tag.putString("id", "minecraft:mob_spawner");
            return tag;
        }
        return MekBox.spawnerTag(seed);
    }
}
