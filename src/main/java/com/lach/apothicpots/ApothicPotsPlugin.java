package com.lach.apothicpots;

import net.darkhax.botanypots.common.api.BotanyPotsPlugin;

public class ApothicPotsPlugin implements BotanyPotsPlugin {
    @Override
    public void registerDisplayTypes() {
        SpawnerDisplay.register();
    }

    @Override
    public void bindDisplayRenderers() {
        com.lach.apothicpots.client.SpawnerDisplayRenderer.bind();
    }
}
