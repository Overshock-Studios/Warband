package com.warband.illager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent player-facing pressure from an illager faction. */
public record FactionReputation(IllagerFaction faction, int heat, long bountyReadyAt,
                                long warReadyAt, long crusadeReadyAt) {

    public static final Codec<FactionReputation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IllagerFaction.CODEC.fieldOf("faction").forGetter(FactionReputation::faction),
            Codec.INT.fieldOf("heat").forGetter(FactionReputation::heat),
            Codec.LONG.optionalFieldOf("bountyReadyAt", 0L).forGetter(FactionReputation::bountyReadyAt),
            Codec.LONG.optionalFieldOf("warReadyAt", 0L).forGetter(FactionReputation::warReadyAt),
            Codec.LONG.optionalFieldOf("crusadeReadyAt", 0L).forGetter(FactionReputation::crusadeReadyAt)
    ).apply(instance, FactionReputation::new));

    public FactionReputation(IllagerFaction faction, int heat, long bountyReadyAt) {
        this(faction, heat, bountyReadyAt, 0L, 0L);
    }

    public FactionReputation addHeat(int amount, long readyAt) {
        return new FactionReputation(faction, Math.min(200, heat + amount),
                minReady(bountyReadyAt, readyAt), warReadyAt, crusadeReadyAt);
    }

    public FactionReputation coolDown(long nextReadyAt) {
        return new FactionReputation(faction, Math.max(0, heat - 60), nextReadyAt, warReadyAt, crusadeReadyAt);
    }

    public FactionReputation withWarReady(long readyAt) {
        return new FactionReputation(faction, heat, bountyReadyAt, readyAt, crusadeReadyAt);
    }

    public FactionReputation withCrusadeReady(long readyAt) {
        return new FactionReputation(faction, heat, bountyReadyAt, warReadyAt, readyAt);
    }

    public VengeanceState state() {
        return VengeanceState.fromHeat(heat);
    }

    private static long minReady(long current, long next) {
        return current <= 0L ? next : Math.min(current, next);
    }
}
