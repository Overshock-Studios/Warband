package com.warband.net;

import com.warband.difficulty.DifficultyManager;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side of the difficulty lens — pushes each online player their local
 * difficulty and capability score once a second, for the HUD to render.
 */
public final class DifficultyLensSync {

    private static final int SEND_INTERVAL_TICKS = 20;

    private static int tickCounter;

    private DifficultyLensSync() {
    }

    /** Register the per-second sync tick. Called from {@code onInitialize}. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < SEND_INTERVAL_TICKS) return;
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerLevel level = (ServerLevel) player.level();
                double difficulty = DifficultyManager.getDifficulty(level, player.blockPosition(), player);
                Float score = player.getAttached(WarbandAttachments.PLAYER_SCORE);
                ServerPlayNetworking.send(player, new DifficultyLensPayload(
                        (float) difficulty, score != null ? score : 0.0f));
            }
        });
    }
}
