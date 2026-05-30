package com.warband.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Soft detection for Stormie's Spiders. The mod replaces vanilla spider
 * pathfinding/climbing behavior, so we step back from features that overlap
 * (ceiling crawl) and let theirs own that domain. Features that don't overlap
 * (web throws, sticky path, leap-strike) keep running on top.
 *
 * <p>No compile-time dependency — checked once at static-init time.
 */
public final class StormieSpidersCompat {

    private static final String MOD_ID = "stormies_spiders";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

    private StormieSpidersCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}
