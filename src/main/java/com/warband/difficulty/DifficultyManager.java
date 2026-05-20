package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The single source of truth for local difficulty.
 *
 * <p>Everything else — stat buffs, AI tier, squad size, spawn pacing — reads
 * one normalized scalar from here: {@code 0.0} is
 * vanilla-calm, {@code 1.0} is maximum. Keep it that way; one scalar in, many
 * systems out. The intent is also to stamp this value onto each mob (as a data
 * component) at spawn, so a mob carries its own difficulty and other mods can
 * read it.
 */
public final class DifficultyManager {

    private DifficultyManager() {
    }

    /** Local difficulty at a position, normalized {@code 0.0 .. 1.0}. */
    public static double getDifficulty(ServerLevel level, BlockPos pos) {
        return switch (WarbandConfig.difficultyMode) {
            case DISTANCE -> distanceDifficulty(level, pos);
            case TIME -> timeDifficulty(level);
            case SCORE -> 0.0; // TODO: SCORE needs a player — add getDifficulty(level, pos, player)
            case COMBINED -> Math.max(distanceDifficulty(level, pos), timeDifficulty(level));
        };
    }

    private static double distanceDifficulty(ServerLevel level, BlockPos pos) {
        BlockPos spawn = level.getRespawnData().pos();
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        double safe = WarbandConfig.safeRadius;
        double max = Math.max(safe + 1.0, WarbandConfig.maxDifficultyRadius);
        return clamp01((dist - safe) / (max - safe));
    }

    private static double timeDifficulty(ServerLevel level) {
        long day = level.getLevelData().getGameTime() / 24000L;
        double maxDays = Math.max(1.0, WarbandConfig.maxDifficultyDays);
        return clamp01(day / maxDays);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
