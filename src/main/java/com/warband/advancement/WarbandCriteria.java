package com.warband.advancement;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;

/** Holds Warband's custom advancement triggers and the string kinds they fire with. */
public final class WarbandCriteria {

    public static final String FACTION_NOTICED = "faction_noticed";
    public static final String FIRST_GRUDGE = "first_grudge";
    public static final String FIRST_REVENGE = "first_revenge";
    public static final String BOUNTY_SUMMONED = "bounty_summoned";
    public static final String WARMARSHAL_SLAIN = "warmarshal_slain";
    public static final String FACTION_AT_WAR = "faction_at_war";
    public static final String CRUSADE_CALLED = "crusade_called";

    public static final WarbandEventTrigger EVENT = CriteriaTriggers.register(
            "warband:event", new WarbandEventTrigger());

    private WarbandCriteria() {
    }

    public static void init() {
        // Class-load triggers the static field above. Called from onInitialize.
    }

    public static void fire(ServerPlayer player, String kind) {
        EVENT.trigger(player, kind);
    }
}
