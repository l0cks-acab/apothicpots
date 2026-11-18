package com.lach.apothicpots.compat;

import com.lach.apothicpots.DropTable.Drop;
import com.lach.apothicpots.DropTable.MobDrops;
import com.lach.apothicpots.SpawnerCrop;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpawnerPotCategory implements IRecipeCategory<MobDrops> {
    private static final int W = 152, H = 68, OUT_X = 76;
    private final IDrawable icon;
    private final Map<EntityType<?>, LivingEntity> entities = new HashMap<>();

    public SpawnerPotCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Items.SPAWNER);
    }

    @Override public RecipeType<MobDrops> getRecipeType() { return ApothicJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.apothicpots.spawner_pot"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return W; }
    @Override public int getHeight() { return H; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobDrops recipe, IFocusGroup focuses) {
        SpawnEggItem egg = SpawnEggItem.byId(recipe.type());
        if (egg != null) builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).setStandardSlotBackground().addItemStack(new ItemStack(egg));
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 23).setStandardSlotBackground().addItemStack(new ItemStack(Items.SPAWNER));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 45).setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(Items.SOUL_SAND), new ItemStack(Items.SOUL_SOIL)));
        List<Drop> drops = recipe.drops();
        for (int i = 0; i < drops.size() && i < 12; i++) {
            Drop drop = drops.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + (i % 4) * 18, 1 + (i / 4) * 18).setStandardSlotBackground()
                    .addItemStack(drop.stack())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.apothicpots.per_kill",
                            String.format(java.util.Locale.ROOT, "%.2f", drop.perKill())).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(MobDrops recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        LivingEntity entity = entity(recipe.type());
        if (entity != null) {
            float size = Math.max(entity.getBbHeight(), entity.getBbWidth());
            float scale = Math.min(26f, 34f / Math.max(0.4f, size));
            float angle = (System.currentTimeMillis() % 7200L) / 20f;
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(angle));
            try {
                InventoryScreen.renderEntityInInventory(g, 46, 50, scale, new Vector3f(0, 0, 0), pose, null, entity);
            } catch (Exception ignored) {
                entities.put(recipe.type(), null);
            }
        }
        Font font = Minecraft.getInstance().font;
        float tickrate = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.tickRateManager().tickrate() : 20f;
        Component line = Component.translatable("jei.apothicpots.summary",
                StringUtil.formatTickDuration(recipe.cycleTicks(), tickrate), SpawnerCrop.fmt(recipe.killsPerCycle()),
                SpawnerCrop.fmt(Math.round(recipe.xpPerKill() * recipe.killsPerCycle() * 10) / 10.0));
        g.drawString(font, line, 22, 58, 0xFF606060, false);
    }

    private LivingEntity entity(EntityType<?> type) {
        if (entities.containsKey(type)) return entities.get(type);
        LivingEntity living = null;
        var level = Minecraft.getInstance().level;
        if (level != null) {
            try {
                Entity e = type.create(level);
                if (e instanceof LivingEntity l) living = l;
            } catch (Exception ignored) {}
        }
        entities.put(type, living);
        return living;
    }
}
