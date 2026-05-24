package com.warband.illager;

import com.mojang.serialization.Codec;

public enum FactionDoctrine {
    SIEGE,
    HUNT,
    BURN,
    COMMAND,
    AMBUSH;

    public static final Codec<FactionDoctrine> CODEC = Codec.STRING.xmap(
            value -> FactionDoctrine.valueOf(value.toUpperCase()),
            doctrine -> doctrine.name().toLowerCase()
    );
}
