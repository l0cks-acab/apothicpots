package com.lach.apothicpots.compat;

import com.lach.apothicpots.ApothicPots;
import com.lach.apothicpots.PotLines;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(ApothicPots.MODID)
public class JadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = ApothicPots.id("spawner_pot");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, BotanyPotBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, BotanyPotBlock.class);
    }

    enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            PotLines.lines(accessor.getBlockEntity(), accessor.getServerData(), accessor.tickRate()).forEach(tooltip::add);
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            PotLines.writeServerData(tag, accessor.getBlockEntity());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
