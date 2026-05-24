package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Runtime chunk memory of player capability pressure. */
public final class RegionalDifficulty {

    private static final int SAMPLE_INTERVAL_TICKS = 20 * 5;
    private static final int SAVE_INTERVAL_TICKS = 20 * 60;
    private static final int EXPIRE_TICKS = 20 * 60 * 60;
    private static final String SAVE_FILE = "warband-regional-difficulty.csv";
    private static final Map<String, Map<Long, Cell>> BY_DIMENSION = new HashMap<>();

    private static int tickCounter;
    private static int saveCounter;

    private RegionalDifficulty() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(RegionalDifficulty::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(RegionalDifficulty::save);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < SAMPLE_INTERVAL_TICKS) return;
            tickCounter = 0;

            long now = server.overworld().getGameTime();
            sampleAll(server, now);
            decayAndTrim(now);
            if ((saveCounter += SAMPLE_INTERVAL_TICKS) >= SAVE_INTERVAL_TICKS) {
                saveCounter = 0;
                save(server);
            }
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

    public static String mapAround(ServerLevel level, BlockPos pos, int radiusChunks) {
        Map<Long, Cell> cells = BY_DIMENSION.get(dimensionKey(level));
        int centerX = pos.getX() >> 4;
        int centerZ = pos.getZ() >> 4;
        StringBuilder out = new StringBuilder();
        for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            if (dz > -radiusChunks) out.append('\n');
            for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
                Cell cell = cells == null ? null : cells.get(chunkKey(centerX + dx, centerZ + dz));
                out.append(cellSymbol(cell == null ? 0.0 : cell.value));
            }
        }
        return out.toString();
    }

    public static int knownCells(ServerLevel level) {
        Map<Long, Cell> cells = BY_DIMENSION.get(dimensionKey(level));
        return cells == null ? 0 : cells.size();
    }

    public static double rawCellValue(ServerLevel level, BlockPos pos) {
        Map<Long, Cell> cells = BY_DIMENSION.get(dimensionKey(level));
        if (cells == null) return 0.0;
        Cell cell = cells.get(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
        return cell == null ? 0.0 : clamp01(cell.value);
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
                Cell cell = cells.getOrDefault(cellEntry.getKey(), new Cell(0.0, now, 0));
                int pressureSamples = now - cell.lastTouched <= SAMPLE_INTERVAL_TICKS * 2L
                        ? cell.pressureSamples + 1
                        : 1;
                double acceleration = Math.min(WarbandConfig.regionalAccelerationMax,
                        pressureSamples * WarbandConfig.regionalAccelerationPerSample);
                double blend = clamp01(WarbandConfig.regionalBlendRate + acceleration);
                double updated = cell.value + (target - cell.value) * blend;
                cells.put(cellEntry.getKey(), new Cell(clamp01(updated), now, pressureSamples));
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
                    int pressureSamples = Math.max(0, cell.pressureSamples - 1);
                    entry.setValue(new Cell(cell.value * (1.0 - WarbandConfig.regionalDecayRate), cell.lastTouched, pressureSamples));
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

    private static char cellSymbol(double value) {
        if (value < 0.05) return '.';
        if (value < 0.25) return '1';
        if (value < 0.50) return '2';
        if (value < 0.75) return '3';
        if (value < 0.95) return '4';
        return '5';
    }

    private static void load(MinecraftServer server) {
        Path path = savePath(server);
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(",", 5);
                if (parts.length != 5) continue;
                String dimension = parts[0];
                long key = Long.parseLong(parts[1]);
                double value = Double.parseDouble(parts[2]);
                long lastTouched = Long.parseLong(parts[3]);
                int pressureSamples = Integer.parseInt(parts[4]);
                BY_DIMENSION.computeIfAbsent(dimension, ignored -> new HashMap<>())
                        .put(key, new Cell(clamp01(value), lastTouched, Math.max(0, pressureSamples)));
            }
        } catch (IOException | NumberFormatException ignored) {
            BY_DIMENSION.clear();
        }
    }

    private static void save(MinecraftServer server) {
        StringBuilder out = new StringBuilder("# dimension,chunkKey,value,lastTouched,pressureSamples\n");
        for (Map.Entry<String, Map<Long, Cell>> dimEntry : BY_DIMENSION.entrySet()) {
            for (Map.Entry<Long, Cell> cellEntry : dimEntry.getValue().entrySet()) {
                Cell cell = cellEntry.getValue();
                out.append(dimEntry.getKey()).append(',')
                        .append(cellEntry.getKey()).append(',')
                        .append(cell.value).append(',')
                        .append(cell.lastTouched).append(',')
                        .append(cell.pressureSamples).append('\n');
            }
        }
        try {
            Files.writeString(savePath(server), out.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Difficulty memory is balance state, not critical save data.
        }
    }

    private static Path savePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(SAVE_FILE);
    }

    private record Cell(double value, long lastTouched, int pressureSamples) {
    }
}
