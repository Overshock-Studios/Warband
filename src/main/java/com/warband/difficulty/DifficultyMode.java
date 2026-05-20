package com.warband.difficulty;

/**
 * How local difficulty is derived. Set via config; read by
 * {@link DifficultyManager}.
 */
public enum DifficultyMode {

    /** Difficulty rises with distance from world spawn. */
    DISTANCE,

    /** Difficulty follows a running average of player capability sampled into chunks. */
    REGIONAL;

    public static DifficultyMode fromString(String raw, DifficultyMode fallback) {
        if (raw == null) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
