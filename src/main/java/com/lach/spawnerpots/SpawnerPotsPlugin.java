package com.lach.spawnerpots;

import net.darkhax.botanypots.common.api.BotanyPotsPlugin;

public class SpawnerPotsPlugin implements BotanyPotsPlugin {
    @Override
    public void registerDisplayTypes() {
        SpawnerDisplay.register();
    }

    @Override
    public void bindDisplayRenderers() {
        com.lach.spawnerpots.client.SpawnerDisplayRenderer.bind();
    }
}
