package com.warband.illager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistent player-scoped memory for illager revenge patrols.
 *
 * <p>{@code originPos}/{@code originDimension} record where the original fight
 * happened, so revenge can muster from that place rather than teleporting near
 * the player. They are plain coordinates — no structure lookup — so this works
 * with any pillager structure, vanilla or modded.
 */
public record IllagerGrudge(String survivorName, IllagerFaction faction, float difficulty,
                            int anger, long readyAt, int attempts,
                            long originPos, String originDimension) {

    public static final Codec<IllagerGrudge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("survivorName").forGetter(IllagerGrudge::survivorName),
            IllagerFaction.CODEC.optionalFieldOf("faction", IllagerFaction.BLACK_HORN).forGetter(IllagerGrudge::faction),
            Codec.FLOAT.fieldOf("difficulty").forGetter(IllagerGrudge::difficulty),
            Codec.INT.fieldOf("anger").forGetter(IllagerGrudge::anger),
            Codec.LONG.fieldOf("readyAt").forGetter(IllagerGrudge::readyAt),
            Codec.INT.optionalFieldOf("attempts", 0).forGetter(IllagerGrudge::attempts),
            Codec.LONG.optionalFieldOf("originPos", 0L).forGetter(IllagerGrudge::originPos),
            Codec.STRING.optionalFieldOf("originDimension", "").forGetter(IllagerGrudge::originDimension)
    ).apply(instance, IllagerGrudge::new));

    public IllagerGrudge addAnger(int amount, long newReadyAt) {
        return new IllagerGrudge(survivorName, faction, difficulty, Math.min(100, anger + amount),
                newReadyAt, attempts, originPos, originDimension);
    }

    public IllagerGrudge attempted(long retryAt) {
        return new IllagerGrudge(survivorName, faction, difficulty, Math.max(0, anger - 20),
                retryAt, attempts + 1, originPos, originDimension);
    }

    /** True if a fight location was recorded for this grudge. */
    public boolean hasOrigin() {
        return !originDimension.isEmpty();
    }
}
