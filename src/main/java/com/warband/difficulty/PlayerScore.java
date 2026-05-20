package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player capability score — the input to {@link DifficultyMode#SCORE}.
 *
 * <p>The score is a stored {@code 0.0..1.0} value, not a live reading. Each
 * second it is compared against a fresh capability <i>sample</i> (gear: armor,
 * toughness, attack damage) and updated:
 * <ul>
 *   <li><b>Ratchet up</b> — if the sample is higher, the score jumps to it at
 *       once; gearing up should register immediately.</li>
 *   <li><b>Decay down</b> — if the sample is lower, the score drifts toward it
 *       slowly (see {@code scoreDecayRate}).</li>
 * </ul>
 * This makes difficulty track durable capability: a transient sword-swap never
 * moves it, but genuinely falling off eases difficulty over minutes.
 *
 * <p>Post-death <b>relief</b> layers on top: a real death stamps a relief
 * deadline ({@link WarbandAttachments#DEATH_RELIEF}) and the score is scaled
 * down until that tick passes.
 *
 * <p>TODO: fold in a progression term (advancements earned) once Phase 3 adds a
 * reason to track it — gear alone underrates a skilled but lightly-equipped
 * player.
 */
public final class PlayerScore {

    // Reference maxima for a fully geared player. Enchantments are intentionally
    // ignored so late-game enchants remain player power instead of raising mobs.
    private static final double ARMOR_MAX = 20.0;
    private static final double TOUGHNESS_MAX = 12.0;
    private static final double ATTACK_SPAN = 8.0; // damage above the unarmed base of 1.0

    /** How often the stored score is re-evaluated, in ticks. */
    private static final int UPDATE_INTERVAL_TICKS = 20;

    private static int tickCounter;

    private PlayerScore() {
    }

    /** Register the death-relief listener and the score-update tick. */
    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive == false means an actual death (not an End-portal return).
            if (!alive && WarbandConfig.deathReliefSeconds > 0) {
                long until = newPlayer.level().getGameTime()
                        + (long) WarbandConfig.deathReliefSeconds * 20L;
                newPlayer.setAttached(WarbandAttachments.DEATH_RELIEF, until);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < UPDATE_INTERVAL_TICKS) return;
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateScore(player);
            }
        });
    }

    /** This player's contribution to local difficulty, normalized {@code 0.0..1.0}. */
    public static double difficultyFor(Player player) {
        Float stored = player.getAttached(WarbandAttachments.PLAYER_SCORE);
        double score = stored != null ? stored : 0.0;

        Long reliefUntil = player.getAttached(WarbandAttachments.DEATH_RELIEF);
        if (reliefUntil != null && player.level().getGameTime() < reliefUntil) {
            score *= 1.0 - WarbandConfig.deathReliefStrength;
        }
        return clamp01(score);
    }

    /** Ratchet the stored score up to, or decay it toward, the current sample. */
    private static void updateScore(ServerPlayer player) {
        double sample = sampleCapability(player);
        Float stored = player.getAttached(WarbandAttachments.PLAYER_SCORE);
        double current = stored != null ? stored : 0.0;

        double updated;
        if (sample >= current) {
            updated = sample; // instant ratchet up
        } else {
            updated = current + (sample - current) * WarbandConfig.scoreDecayRate;
        }
        player.setAttached(WarbandAttachments.PLAYER_SCORE, (float) clamp01(updated));
    }

    /** A live {@code 0.0..1.0} reading of how well-equipped the player is right now. */
    private static double sampleCapability(Player player) {
        double armor = clamp01(player.getAttributeValue(Attributes.ARMOR) / ARMOR_MAX);
        double toughness = clamp01(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) / TOUGHNESS_MAX);
        double attack = clamp01((player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0) / ATTACK_SPAN);
        return clamp01(0.45 * armor + 0.25 * toughness + 0.30 * attack);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
