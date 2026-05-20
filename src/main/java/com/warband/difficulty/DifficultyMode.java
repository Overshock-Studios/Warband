package com.warband.difficulty;

/**
 * How local difficulty is derived. Set via config; read by
 * {@link DifficultyManager}.
 */
public enum DifficultyMode {

    /** Difficulty rises with distance from world spawn. The default. */
    DISTANCE,

    /** Difficulty rises as the world ages (day count). */
    TIME,

    /** Difficulty rises with a per-player capability score — see {@link PlayerScore}. */
    SCORE,

    /** Combination — the highest of the enabled modes wins. */
    COMBINED;

    public static DifficultyMode fromString(String raw, DifficultyMode fallback) {
        if (raw == null) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
