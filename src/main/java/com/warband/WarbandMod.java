package com.warband;

import com.warband.ally.GolemDirector;
import com.warband.command.WarbandCommand;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TemporaryTacticBlocks;
import com.warband.config.WarbandConfig;
import com.warband.difficulty.PlayerScore;
import com.warband.entity.WarbandAttachments;
import com.warband.net.DifficultyLensPayload;
import com.warband.net.DifficultyLensSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
        WarbandAttachments.init();
        PlayerScore.register();
        WarbandCommand.register();
        TemporaryTacticBlocks.register();
        SquadCoordinator.register();
        GolemDirector.register();

        PayloadTypeRegistry.clientboundPlay().register(DifficultyLensPayload.TYPE, DifficultyLensPayload.CODEC);
        DifficultyLensSync.register();

        LOGGER.info("[Warband] initialized");
    }
}
