package com.lach.apothicpots.client;

import com.lach.apothicpots.SpawnerDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.darkhax.botanypots.common.api.data.display.render.DisplayRenderer;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class SpawnerDisplayRenderer extends DisplayRenderer<SpawnerDisplay> {
    /** Cage size relative to a full block; fits inside the pot rim. */
    private static final float CAGE = 0.625f;
    private static final float SPIN_PER_TICK = 1.5f;

    private final Map<CompoundTag, Entity> entities = new HashMap<>();
    private Level cachedLevel;

    public static void bind() {
        DisplayRenderer.bind(SpawnerDisplay.type(), new SpawnerDisplayRenderer());
    }

    @Override
    public float render(Context context, SpawnerDisplay display, PoseStack pose, Level level, BlockPos pos, float tickDelta,
                        MultiBufferSource buffers, int light, int overlay, BotanyPotBlockEntity pot, float progress,
                        float growthScale, float heightOffset) {
        pose.pushPose();
        pose.translate(0.5f - CAGE / 2f, heightOffset, 0.5f - CAGE / 2f);
        pose.scale(CAGE, CAGE, CAGE);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.SPAWNER.defaultBlockState(), pose, buffers, light, overlay);
        Entity entity = entity(level, display.entity());
        if (entity != null) {
            double spin = (level.getGameTime() % 24000L) * SPIN_PER_TICK;
            SpawnerRenderer.renderEntityInSpawner(tickDelta, pose, buffers, light, entity, context.getEntityRenderer(), spin - SPIN_PER_TICK, spin);
        }
        pose.popPose();
        return heightOffset + CAGE;
    }

    private Entity entity(Level level, CompoundTag tag) {
        if (level != cachedLevel) {
            entities.clear();
            cachedLevel = level;
        }
        if (entities.size() > 256) entities.clear();
        return entities.computeIfAbsent(tag, t -> EntityType.loadEntityRecursive(t.copy(), level, e -> e));
    }
}
