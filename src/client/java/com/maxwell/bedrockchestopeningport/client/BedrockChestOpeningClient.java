package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.DataUtil.DungeonBossLootManager;
import com.maxwell.bedrockchestopeningport.client.config.MyConfig;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.neoforged.fml.config.ModConfig;

public class BedrockChestOpeningClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register(
                "bedrock-chest-opening",
                ModConfig.Type.CLIENT,
                MyConfig.SPEC
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ChestListener.reset();
        });
        DungeonBossLootManager.initialize();
    }
}