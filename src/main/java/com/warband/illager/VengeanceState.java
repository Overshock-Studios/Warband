package com.warband.illager;

/** Faction-wide posture toward a player, derived from accumulated heat. */
public enum VengeanceState {
    QUIET("quiet"),
    NOTICED("noticed"),
    WARY("watching"),
    HOSTILE("hostile"),
    WAR("at war"),
    CRUSADE("crusade");

    private final String label;

    VengeanceState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static VengeanceState fromHeat(int heat) {
        if (heat >= 150) return CRUSADE;
        if (heat >= 80) return WAR;
        if (heat >= 40) return HOSTILE;
        if (heat >= 20) return WARY;
        if (heat >= 1) return NOTICED;
        return QUIET;
    }
}
