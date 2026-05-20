package com.warband.illager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent player-facing pressure from an illager faction. */
public record FactionReputation(IllagerFaction faction, int heat, long bountyReadyAt) {

    public static final Codec<FactionReputation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IllagerFaction.CODEC.fieldOf("faction").forGetter(FactionReputation::faction),
            Codec.INT.fieldOf("heat").forGetter(FactionReputation::heat),
            Codec.LONG.optionalFieldOf("bountyReadyAt", 0L).forGetter(FactionReputation::bountyReadyAt)
    ).apply(instance, FactionReputation::new));

    public FactionReputation addHeat(int amount, long readyAt) {
        return new FactionReputation(faction, Math.min(200, heat + amount), minReady(bountyReadyAt, readyAt));
    }

    public FactionReputation coolDown(long nextReadyAt) {
        return new FactionReputation(faction, Math.max(0, heat - 60), nextReadyAt);
    }

    private static long minReady(long current, long next) {
        return current <= 0L ? next : Math.min(current, next);
    }
}
