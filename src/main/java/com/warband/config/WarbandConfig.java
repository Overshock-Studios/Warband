package com.warband.config;

import com.warband.difficulty.DifficultyMode;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * File-backed config at {@code config/warband.properties}.
 *
 * <p>Written with {@link Files#writeString} from a text block — never
 * {@code Properties.store()} (timestamp + encoding noise on Windows).
 */
public final class WarbandConfig {

    // ── Difficulty ──────────────────────────────────────────────────────────
    public static DifficultyMode difficultyMode = DifficultyMode.SCORE;
    /** Blocks from world spawn that stay fully vanilla (difficulty 0). */
    public static int safeRadius = 256;
    /** Distance from spawn at which difficulty caps (DISTANCE mode). */
    public static int maxDifficultyRadius = 4096;
    /** World day count at which difficulty caps (TIME mode). */
    public static int maxDifficultyDays = 30;
    /** If true, Peaceful disables Warband and Easy/Normal lower its ceiling. */
    public static boolean respectGlobalDifficulty = true;
    /** If true, vanilla regional difficulty feeds COMBINED as an extra term. */
    public static boolean factorVanillaDifficulty = false;
    /** Seconds of eased difficulty after a player death. 0 disables relief. */
    public static int deathReliefSeconds = 600;
    /** How much relief eases difficulty: 0.0 none .. 1.0 full calm. */
    public static double deathReliefStrength = 0.5;

    // ── Squads & spawning ───────────────────────────────────────────────────
    public static boolean squadsEnabled = true;
    public static int maxSquadSize = 6;
    /** Performance cap — most "smart AI" mobs ticked per player at once. */
    public static int maxSmartMobsPerPlayer = 24;

    private static final Path CONFIG_PATH = Path.of("config", "warband.properties");

    private WarbandConfig() {
    }

    public static void load(Logger logger) {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException e) {
                logger.error("[Warband] Failed to read config, using defaults", e);
            }
        }

        difficultyMode = DifficultyMode.fromString(props.getProperty("difficultyMode"), difficultyMode);
        safeRadius = parseInt(props, "safeRadius", safeRadius, 0, 100_000, logger);
        maxDifficultyRadius = parseInt(props, "maxDifficultyRadius", maxDifficultyRadius, 1, 1_000_000, logger);
        maxDifficultyDays = parseInt(props, "maxDifficultyDays", maxDifficultyDays, 1, 100_000, logger);
        respectGlobalDifficulty = parseBoolean(props, "respectGlobalDifficulty", respectGlobalDifficulty, logger);
        factorVanillaDifficulty = parseBoolean(props, "factorVanillaDifficulty", factorVanillaDifficulty, logger);
        deathReliefSeconds = parseInt(props, "deathReliefSeconds", deathReliefSeconds, 0, 100_000, logger);
        deathReliefStrength = parseDouble(props, "deathReliefStrength", deathReliefStrength, 0.0, 1.0, logger);

        squadsEnabled = parseBoolean(props, "squadsEnabled", squadsEnabled, logger);
        maxSquadSize = parseInt(props, "maxSquadSize", maxSquadSize, 1, 64, logger);
        maxSmartMobsPerPlayer = parseInt(props, "maxSmartMobsPerPlayer", maxSmartMobsPerPlayer, 1, 512, logger);

        save(logger);
        logger.info("[Warband] Config loaded");
    }

    public static void save(Logger logger) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, toPropertiesString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("[Warband] Failed to save config", e);
        }
    }

    private static String toPropertiesString() {
        return """
                # Warband configuration
                # Changes take effect on world reload or server restart.

                # ── Difficulty ────────────────────────────────────────────────────
                # How local difficulty is derived: DISTANCE, TIME, SCORE, or COMBINED.
                difficultyMode=%s
                # Blocks from world spawn that stay fully vanilla (difficulty 0).
                safeRadius=%d
                # Distance from spawn at which difficulty caps (DISTANCE mode).
                maxDifficultyRadius=%d
                # World day count at which difficulty caps (TIME mode).
                maxDifficultyDays=%d
                # Peaceful disables Warband; Easy/Normal lower its difficulty ceiling.
                respectGlobalDifficulty=%s
                # Fold vanilla regional difficulty into COMBINED mode as an extra term.
                factorVanillaDifficulty=%s
                # Seconds of eased difficulty after a player death (0 disables).
                deathReliefSeconds=%d
                # How much a death eases difficulty: 0.0 none .. 1.0 full calm.
                deathReliefStrength=%s

                # ── Squads & spawning ─────────────────────────────────────────────
                # If true, mobs may spawn as role-based squads at higher difficulty.
                squadsEnabled=%s
                # Largest squad that can spawn.
                maxSquadSize=%d
                # Performance cap: most tactical-AI mobs ticked per player at once.
                maxSmartMobsPerPlayer=%d
                """.formatted(
                    difficultyMode,
                    safeRadius,
                    maxDifficultyRadius,
                    maxDifficultyDays,
                    respectGlobalDifficulty,
                    factorVanillaDifficulty,
                    deathReliefSeconds,
                    deathReliefStrength,
                    squadsEnabled,
                    maxSquadSize,
                    maxSmartMobsPerPlayer
                );
    }

    private static boolean parseBoolean(Properties props, String key, boolean def, Logger logger) {
        String raw = props.getProperty(key);
        if (raw == null) return def;
        String s = raw.trim().toLowerCase();
        if (s.equals("true")) return true;
        if (s.equals("false")) return false;
        logger.warn("[Warband] '{}' is not a valid boolean ('{}'), using default {}", key, raw, def);
        return def;
    }

    private static double parseDouble(Properties props, String key, double def, double min, double max, Logger logger) {
        String raw = props.getProperty(key);
        if (raw == null) return def;
        try {
            double val = Double.parseDouble(raw.trim());
            if (val < min || val > max) {
                logger.warn("[Warband] '{}' value {} out of range [{}, {}], clamping", key, val, min, max);
                return Math.max(min, Math.min(max, val));
            }
            return val;
        } catch (NumberFormatException e) {
            logger.warn("[Warband] '{}' is not a valid number ('{}'), using default {}", key, raw, def);
            return def;
        }
    }

    private static int parseInt(Properties props, String key, int def, int min, int max, Logger logger) {
        String raw = props.getProperty(key);
        if (raw == null) return def;
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < min || val > max) {
                logger.warn("[Warband] '{}' value {} out of range [{}, {}], clamping", key, val, min, max);
                return Math.max(min, Math.min(max, val));
            }
            return val;
        } catch (NumberFormatException e) {
            logger.warn("[Warband] '{}' is not a valid integer ('{}'), using default {}", key, raw, def);
            return def;
        }
    }
}
