package com.lach.apothicpots;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/** Mekanism cardboard box holding a spawner. Soft support: read through the registry, no compile dep. */
public final class MekBox {
    private static final ResourceLocation BOX = ResourceLocation.fromNamespaceAndPath("mekanism", "cardboard_box");
    private static final ResourceLocation BLOCK_DATA = ResourceLocation.fromNamespaceAndPath("mekanism", "block_data");
    private static final String SPAWNER = "minecraft:spawner";

    private MekBox() {}

    @Nullable
    private static Item boxItem() {
        return BuiltInRegistries.ITEM.getOptional(BOX).orElse(null);
    }

    public static boolean isBox(ItemStack stack) {
        Item box = boxItem();
        return box != null && stack.is(box);
    }

    /** Spawner block entity tag inside a boxed spawner, or null. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static CompoundTag spawnerTag(ItemStack stack) {
        if (stack.isEmpty() || !isBox(stack)) return null;
        DataComponentType<Object> type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(BLOCK_DATA);
        if (type == null || type.codec() == null) return null;
        Object data = stack.get(type);
        if (data == null) return null;
        Tag enc = type.codec().encodeStart(NbtOps.INSTANCE, data).result().orElse(null);
        if (!(enc instanceof CompoundTag root)) return null;
        if (!SPAWNER.equals(root.getCompound("state").getString("Name"))) return null;
        if (!root.contains("block_entity_tag", Tag.TAG_COMPOUND)) return null;
        return root.getCompound("block_entity_tag").copy();
    }

    public static boolean isBoxedSpawner(ItemStack stack) {
        return SpawnerInfo.of(stack) != null && isBox(stack);
    }

    /** Plain spawner item carrying the boxed spawner's data, same as a silk-touched spawner. */
    public static ItemStack unbox(ItemStack stack) {
        CompoundTag tag = spawnerTag(stack);
        if (tag == null) return ItemStack.EMPTY;
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        ItemStack out = new ItemStack(Items.SPAWNER);
        BlockItem.setBlockEntityData(out, BlockEntityType.MOB_SPAWNER, tag);
        return out;
    }

    public static ItemStack emptyBox() {
        Item box = boxItem();
        return box == null ? ItemStack.EMPTY : new ItemStack(box);
    }
}
