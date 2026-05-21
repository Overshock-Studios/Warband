package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The single source of truth for local difficulty.
 *
 * <p>Everything else — stat buffs, AI tier, squad size, spawn pacing — reads one
 * normalized scalar from here: {@code 0.0} is vanilla-calm, {@code 1.0} is
 * maximum. Keep it that way; one scalar in, many systems out. The intent is also
 * to stamp this value onto each mob (see {@link com.warband.entity.MobData}) at
 * spawn, so a mob carries its own difficulty.
 *
 * <p>The vanilla difficulty setting is always honored when
 * {@code respectGlobalDifficulty} is on: Peaceful forces {@code 0.0}, and
 * Easy/Normal lower the base value before harsh dimension bonuses are applied.
 */
public final class DifficultyManager {

    private DifficultyManager() {
    }

    /** Local difficulty at a position, normalized {@code 0.0 .. 1.0}. */
    public static double getDifficulty(ServerLevel level, BlockPos pos) {
        return getDifficulty(level, pos, null);
    }

    /**
     * Local difficulty at a position. The {@code player} argument is accepted
     * for API symmetry but no current mode uses it. Normalized {@code 0.0 .. 1.0}.
     */
    public static double getDifficulty(ServerLevel level, BlockPos pos, @Nullable Player player) {
        Difficulty global = level.getLevelData().getDifficulty();
        if (WarbandConfig.respectGlobalDifficulty && global == Difficulty.PEACEFUL) {
            return 0.0;
        }

        double value = rawDifficulty(level, pos, player);

        if (WarbandConfig.factorVanillaDifficulty) {
            value = Math.max(value, level.getCurrentDifficultyAt(pos).getSpecialMultiplier());
        }
        if (WarbandConfig.respectGlobalDifficulty) {
            value *= globalCeiling(global);
        }
        value += dimensionBonus(level);
        return clamp01(value);
    }

    /** Per-dimension additive pressure — the Nether and End are inherently harsher. */
    private static double dimensionBonus(ServerLevel level) {
        if (level.dimension().equals(Level.NETHER)) {
            return WarbandConfig.netherDifficultyBonus;
        }
        if (level.dimension().equals(Level.END)) {
            return WarbandConfig.endDifficultyBonus;
        }
        return 0.0;
    }

    /** The raw scalar from the configured mode, before vanilla-difficulty adjustment. */
    private static double rawDifficulty(ServerLevel level, BlockPos pos, @Nullable Player player) {
        return switch (WarbandConfig.difficultyMode) {
            case DISTANCE -> distanceDifficulty(level, pos);
            case REGIONAL -> RegionalDifficulty.difficultyAt(level, pos);
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

    /** Easy/Normal lower the ceiling; Hard is full; Peaceful is handled earlier. */
    private static double globalCeiling(Difficulty global) {
        return switch (global) {
            case PEACEFUL -> 0.0;
            case EASY -> 0.6;
            case NORMAL -> 0.85;
            case HARD -> 1.0;
        };
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
