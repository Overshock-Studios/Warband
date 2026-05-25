package com.warband;

import com.warband.ally.GolemDirector;
import com.warband.ai.EndermanProvokeHandler;
import com.warband.ai.FriendlyFireHandler;
import com.warband.ai.MultiplayerDirector;
import com.warband.command.WarbandCommand;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TemporaryTacticBlocks;
import com.warband.config.WarbandConfig;
import com.warband.difficulty.PlayerScore;
import com.warband.difficulty.RegionalDifficulty;
import com.warband.entity.WarbandAttachments;
import com.warband.illager.IllagerGrudgeSystem;
import com.warband.illager.StrongholdGarrison;
import com.warband.item.GoatHornCommand;
import com.warband.spawn.AntiFarmDirector;
import com.warband.spawn.BossDirector;
import com.warband.spawn.EncounterDirector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint, runs on dedicated servers and integrated servers.
 *
 * <p>Warband is a vanilla mob AI and spawning overhaul. The world is calm near
 * spawn and grows deadlier the further out you go (distance mode), or as the
 * world ages / the player's score climbs (time / score modes, see
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
        RegionalDifficulty.register();
        WarbandCommand.register();
        TemporaryTacticBlocks.register();
        EncounterDirector.register();
        AntiFarmDirector.register();
        BossDirector.register();
        MultiplayerDirector.register();
        FriendlyFireHandler.register();
        EndermanProvokeHandler.register();
        GoatHornCommand.register();
        SquadCoordinator.register();
        GolemDirector.register();
        IllagerGrudgeSystem.register();
        StrongholdGarrison.register();

        LOGGER.info("[Warband] initialized");
    }
}
