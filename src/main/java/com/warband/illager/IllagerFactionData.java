package com.warband.illager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IllagerFactionData(IllagerFaction faction, FactionDoctrine doctrine) {

    public static final Codec<IllagerFactionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IllagerFaction.CODEC.fieldOf("faction").forGetter(IllagerFactionData::faction),
            FactionDoctrine.CODEC.fieldOf("doctrine").forGetter(IllagerFactionData::doctrine)
    ).apply(instance, IllagerFactionData::new));

    public static IllagerFactionData of(IllagerFaction faction) {
        return new IllagerFactionData(faction, faction.doctrine());
    }
}
