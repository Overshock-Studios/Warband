package com.warband.config;

public enum ConfigProfile {
    CUSTOM,
    VANILLA_PLUS,
    FANTASY;

    public static ConfigProfile fromString(String raw, ConfigProfile fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String normalized = raw.trim().toUpperCase()
                .replace('+', '_')
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.equals("VANILLA")) return VANILLA_PLUS;
        if (normalized.equals("RPG") || normalized.equals("BALANCED") || normalized.equals("BRUTAL")) return FANTASY;
        if (normalized.equals("SOFT")) return VANILLA_PLUS;
        try {
            return ConfigProfile.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
