package com.lach.apothicpots.compat;

import com.lach.apothicpots.PotLines;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;

public class WthitPlugin implements IWailaCommonPlugin, IWailaClientPlugin {
    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData((IDataProvider<BotanyPotBlockEntity>) WthitPlugin::appendData, BotanyPotBlockEntity.class);
    }

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new Body(), BotanyPotBlock.class);
    }

    private static void appendData(IDataWriter data, IServerAccessor<BotanyPotBlockEntity> accessor, IPluginConfig config) {
        PotLines.writeServerData(data.raw(), accessor.getTarget());
    }

    private static final class Body implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            float tickrate = accessor.getWorld().tickRateManager().tickrate();
            PotLines.lines(accessor.getBlockEntity(), accessor.getData().raw(), tickrate).forEach(tooltip::addLine);
        }
    }
}
