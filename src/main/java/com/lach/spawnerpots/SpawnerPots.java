package com.lach.spawnerpots;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SpawnerPots.MODID)
public class SpawnerPots {
    public static final String MODID = "spawnerpots";

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpawnerCrop>> SPAWNER_CROP =
            SERIALIZERS.register("spawner_crop", () -> SpawnerCrop.SERIALIZER);

    public SpawnerPots(IEventBus modBus, ModContainer container) {
        SERIALIZERS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
