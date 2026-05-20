package com.warband;

import com.warband.client.ClientDifficultyState;
import com.warband.client.DifficultyLensHud;
import com.warband.net.DifficultyLensPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * Client entrypoint — wires the difficulty-lens HUD: receives
 * {@link DifficultyLensPayload} updates from the server, and registers the
 * {@link DifficultyLensHud} overlay.
 */
public final class WarbandClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DifficultyLensPayload.TYPE, (payload, context) ->
                ClientDifficultyState.update(payload.difficulty(), payload.score()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientDifficultyState.clear());

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "difficulty_lens"),
                new DifficultyLensHud());

        WarbandMod.LOGGER.info("[Warband] client initialized");
    }
}
