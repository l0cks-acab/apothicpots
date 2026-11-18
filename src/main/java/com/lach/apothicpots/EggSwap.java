package com.lach.apothicpots;

import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-click a spawner pot with a spawn egg to change the mob, like a placed spawner. Stats stay. */
@EventBusSubscriber(modid = ApothicPots.MODID)
public final class EggSwap {
    private EggSwap() {}

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack egg = event.getItemStack();
        if (!(egg.getItem() instanceof SpawnEggItem eggItem)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof AbstractBotanyPotBlockEntity pot)) return;
        ItemStack seed = pot.getSeedItem();
        // Any spawner works, including an empty one with no mob yet (e.g. from the creative menu).
        if (!seed.is(net.minecraft.world.item.Items.SPAWNER) && MekBox.spawnerTag(seed) == null) return;
        SpawnerInfo info = SpawnerInfo.of(seed);
        EntityType<?> type = eggItem.getType(egg);
        if ((info != null && type == info.type()) || type.is(SpawnerInfo.BLACKLIST) || type.is(SpawnerInfo.AS_BLACKLIST)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        Player player = event.getEntity();

        ItemStack spawner = seed;
        if (MekBox.isBox(seed)) {
            spawner = MekBox.unbox(seed);
            BoxUnpacker.giveBack(player, MekBox.emptyBox());
        }
        CustomData data = spawner.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", id);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entity);
        tag.put("SpawnData", spawnData);
        CompoundTag potential = new CompoundTag();
        potential.put("data", spawnData.copy());
        potential.putInt("weight", 1);
        ListTag potentials = new ListTag();
        potentials.add(potential);
        tag.put("SpawnPotentials", potentials);

        ItemStack out = new ItemStack(Items.SPAWNER);
        BlockItem.setBlockEntityData(out, BlockEntityType.MOB_SPAWNER, tag);
        pot.setSeed(out);
        pot.setChanged();
        if (!player.getAbilities().instabuild) egg.shrink(1);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 0.6f);
    }
}
