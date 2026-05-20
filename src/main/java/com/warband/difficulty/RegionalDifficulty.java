package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Runtime chunk memory of player capability pressure. */
public final class RegionalDifficulty {

    private static final int SAMPLE_INTERVAL_TICKS = 20 * 5;
    private static final int EXPIRE_TICKS = 20 * 60 * 60;
    private static final Map<String, Map<Long, Cell>> BY_DIMENSION = new HashMap<>();

    private static int tickCounter;

    private RegionalDifficulty() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < SAMPLE_INTERVAL_TICKS) return;
            tickCounter = 0;

            long now = server.overworld().getGameTime();
            sampleAll(server, now);
            decayAndTrim(now);
        });
    }

    public static double difficultyAt(ServerLevel level, BlockPos pos) {
        Map<Long, Cell> cells = BY_DIMENSION.get(dimensionKey(level));
        if (cells == null || cells.isEmpty()) return 0.0;

        int centerX = pos.getX() >> 4;
        int centerZ = pos.getZ() >> 4;
        double best = 0.0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Cell cell = cells.get(chunkKey(centerX + dx, centerZ + dz));
                if (cell == null) continue;
                double distancePenalty = 1.0 - 0.15 * (Math.abs(dx) + Math.abs(dz));
                best = Math.max(best, cell.value * distancePenalty);
            }
        }
        return clamp01(best);
    }

    /**
     * Samples every online player into the chunk grid for one interval. Players
     * are pooled per cell so a group raises a region's difficulty above what any
     * one of them would alone: the strongest nearby player sets the baseline,
     * and each additional player adds {@code regionalPlayerBonus}.
     */
    private static void sampleAll(MinecraftServer server, long now) {
        // dimensionKey -> (chunkKey -> {playerCount, strongestContribution})
        Map<String, Map<Long, double[]>> pending = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            double score = PlayerScore.difficultyFor(player);
            if (score <= 0.0) continue;

            Map<Long, double[]> cells = pending.computeIfAbsent(dimensionKey(level), ignored -> new HashMap<>());
            int centerX = player.getBlockX() >> 4;
            int centerZ = player.getBlockZ() >> 4;
            int radius = WarbandConfig.regionalSampleRadiusChunks;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > radius + 0.001) continue;
                    double weight = radius <= 0 ? 1.0 : 1.0 - distance / (radius + 1.0);
                    double contribution = score * weight;
                    long key = chunkKey(centerX + dx, centerZ + dz);
                    double[] acc = cells.computeIfAbsent(key, ignored -> new double[2]);
                    acc[0] += 1.0;
                    acc[1] = Math.max(acc[1], contribution);
                }
            }
        }

        for (Map.Entry<String, Map<Long, double[]>> dimEntry : pending.entrySet()) {
            Map<Long, Cell> cells = BY_DIMENSION.computeIfAbsent(dimEntry.getKey(), ignored -> new HashMap<>());
            for (Map.Entry<Long, double[]> cellEntry : dimEntry.getValue().entrySet()) {
                double target = groupTarget(cellEntry.getValue());
                Cell cell = cells.getOrDefault(cellEntry.getKey(), new Cell(0.0, now));
                double updated = cell.value + (target - cell.value) * WarbandConfig.regionalBlendRate;
                cells.put(cellEntry.getKey(), new Cell(clamp01(updated), now));
            }
        }
    }

    /** Group difficulty target: strongest contribution plus a per-extra-player bonus. */
    private static double groupTarget(double[] accumulator) {
        int playerCount = (int) accumulator[0];
        double strongest = accumulator[1];
        double extra = WarbandConfig.regionalPlayerBonus * Math.max(0, playerCount - 1);
        return clamp01(strongest + extra);
    }

    private static void decayAndTrim(long now) {
        for (Map<Long, Cell> cells : BY_DIMENSION.values()) {
            Iterator<Map.Entry<Long, Cell>> iterator = cells.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Cell> entry = iterator.next();
                Cell cell = entry.getValue();
                if (now - cell.lastTouched > EXPIRE_TICKS || cell.value < 0.01) {
                    iterator.remove();
                    continue;
                }
                if (now > cell.lastTouched) {
                    entry.setValue(new Cell(cell.value * (1.0 - WarbandConfig.regionalDecayRate), cell.lastTouched));
                }
            }
        }
    }

    private static String dimensionKey(ServerLevel level) {
        return level.dimension().toString();
    }

    private static long chunkKey(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private record Cell(double value, long lastTouched) {
    }
}
