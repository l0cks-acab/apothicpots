package com.lach.spawnerpots;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.darkhax.botanypots.common.api.data.display.types.Display;
import net.darkhax.botanypots.common.api.data.display.types.DisplayType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** A mini spawner cage sitting on the soil with the mob spinning inside, like a placed spawner. */
public record SpawnerDisplay(CompoundTag entity) implements Display {
    public static final ResourceLocation TYPE_ID = SpawnerPots.id("spawner");
    public static final MapCodec<SpawnerDisplay> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CompoundTag.CODEC.fieldOf("entity").forGetter(SpawnerDisplay::entity)).apply(i, SpawnerDisplay::new));
    public static final StreamCodec<FriendlyByteBuf, SpawnerDisplay> STREAM =
            ByteBufCodecs.COMPOUND_TAG.map(SpawnerDisplay::new, SpawnerDisplay::entity).cast();
    private static DisplayType<SpawnerDisplay> type;

    public static void register() {
        type = DisplayType.register(TYPE_ID, CODEC, STREAM);
    }

    public static DisplayType<SpawnerDisplay> type() {
        return type;
    }

    @Override
    public DisplayType<?> getType() {
        return type;
    }
}
