package com.warband;

import com.warband.config.WarbandConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint — runs on both client and dedicated server.
 *
 * <p>Warband is a vanilla mob AI and spawning overhaul. The world is calm near
 * spawn and grows deadlier the further out you go (distance mode), or as the
 * world ages / the player's score climbs (time / score modes — see
 * {@link com.warband.difficulty.DifficultyMode}). Difficulty buffs not just mob
 * stats but their AI: squad spawns with combat roles, tactical retreat, calling
 * for backup. Applies to standard overworld, Nether, and End mobs.
 */
public final class WarbandMod implements ModInitializer {

    public static final String MOD_ID = "warband";
    public static final Logger LOGGER = LoggerFactory.getLogger("Warband");

    @Override
    public void onInitialize() {
        WarbandConfig.load(LOGGER);

        // TODO: register the difficulty tick, spawn director, squad coordinator,
        //       and the /warband command here.

        LOGGER.info("[Warband] initialized");
    }
}
