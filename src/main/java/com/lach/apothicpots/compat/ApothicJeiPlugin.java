package com.lach.apothicpots.compat;

import com.lach.apothicpots.ApothicPots;
import com.lach.apothicpots.DropTable;
import com.lach.apothicpots.DropTable.MobDrops;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@JeiPlugin
public class ApothicJeiPlugin implements IModPlugin {
    public static final RecipeType<MobDrops> TYPE = RecipeType.create(ApothicPots.MODID, "spawner_pot", MobDrops.class);
    private static IJeiRuntime runtime;
    private static List<MobDrops> shown = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return ApothicPots.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SpawnerPotCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Items.SPAWNER, TYPE);
        for (String pot : new String[]{"terracotta_botany_pot", "terracotta_hopper_botany_pot"}) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("botanypots", pot));
            if (item != Items.AIR) registration.addRecipeCatalyst(item, TYPE);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        shown = List.of();
        DropTable.onClientUpdate = () -> Minecraft.getInstance().execute(ApothicJeiPlugin::refresh);
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        shown = List.of();
    }

    private static void refresh() {
        if (runtime == null) return;
        List<MobDrops> next = DropTable.client;
        if (next == shown) return;
        var manager = runtime.getRecipeManager();
        if (!shown.isEmpty()) manager.hideRecipes(TYPE, shown);
        if (!next.isEmpty()) manager.addRecipes(TYPE, new ArrayList<>(next));
        shown = next;
    }
}
