package com.warband.config;

public enum ConfigProfile {
    CUSTOM,
    SOFT,
    BALANCED,
    BRUTAL,
    COOP;

    public static ConfigProfile fromString(String raw, ConfigProfile fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return ConfigProfile.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
