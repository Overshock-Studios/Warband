package com.warband;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint — client-only setup.
 *
 * <p>Reserved for the "difficulty lens" HUD (local threat readout — essential
 * since distance-based difficulty is otherwise invisible) and squad/role
 * nameplate rendering.
 */
public final class WarbandClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // TODO: difficulty-lens HUD overlay, role nameplates.
        WarbandMod.LOGGER.info("[Warband] client initialized");
    }
}
