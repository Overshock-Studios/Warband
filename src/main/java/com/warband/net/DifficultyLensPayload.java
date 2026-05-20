package com.warband.net;

import com.warband.WarbandMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client packet feeding the difficulty-lens HUD: the local difficulty
 * and the player's own capability score, both {@code 0.0..1.0}. Sent
 * periodically to each player; the client cannot compute difficulty itself
 * (it has no access to {@code DifficultyManager}'s server-side inputs).
 */
public record DifficultyLensPayload(float difficulty, float score) implements CustomPacketPayload {

    public static final Type<DifficultyLensPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "difficulty_lens"));

    public static final StreamCodec<ByteBuf, DifficultyLensPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DifficultyLensPayload::difficulty,
            ByteBufCodecs.FLOAT, DifficultyLensPayload::score,
            DifficultyLensPayload::new);

    @Override
    public Type<DifficultyLensPayload> type() {
        return TYPE;
    }
}
