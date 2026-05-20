package com.warband.difficulty;

import com.warband.config.WarbandConfig;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player capability score — the input to {@link DifficultyMode#SCORE}.
 *
 * <p>The score is a {@code 0.0..1.0} estimate of how well-equipped a player is,
 * read live from their attribute totals (armor, toughness, attack damage). No
 * event plumbing, no saved data — gear is a robust, immediate capability proxy.
 *
 * <p>Post-death <b>relief</b> temporarily eases difficulty: on a real death the
 * player is stamped with a relief deadline ({@link WarbandAttachments#DEATH_RELIEF}),
 * and the score is scaled down until that tick passes.
 *
 * <p>TODO: fold in a progression term (advancements earned) once Phase 3 adds a
 * reason to track it — gear alone underrates a player who is skilled but
 * lightly equipped.
 */
public final class PlayerScore {

    // Reference maxima for a fully netherite-geared player.
    private static final double ARMOR_MAX = 20.0;
    private static final double TOUGHNESS_MAX = 12.0;
    private static final double ATTACK_SPAN = 8.0; // damage above the unarmed base of 1.0

    private PlayerScore() {
    }

    /** Register the death-relief listener. Called from {@code onInitialize}. */
    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive == false means an actual death (not an End-portal return).
            if (!alive && WarbandConfig.deathReliefSeconds > 0) {
                long until = newPlayer.level().getGameTime()
                        + (long) WarbandConfig.deathReliefSeconds * 20L;
                newPlayer.setAttached(WarbandAttachments.DEATH_RELIEF, until);
            }
        });
    }

    /** This player's contribution to local difficulty, normalized {@code 0.0..1.0}. */
    public static double difficultyFor(Player player) {
        double score = gearScore(player);

        Long reliefUntil = player.getAttached(WarbandAttachments.DEATH_RELIEF);
        if (reliefUntil != null && player.level().getGameTime() < reliefUntil) {
            score *= 1.0 - WarbandConfig.deathReliefStrength;
        }
        return clamp01(score);
    }

    private static double gearScore(Player player) {
        double armor = clamp01(player.getAttributeValue(Attributes.ARMOR) / ARMOR_MAX);
        double toughness = clamp01(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) / TOUGHNESS_MAX);
        double attack = clamp01((player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0) / ATTACK_SPAN);
        return clamp01(0.45 * armor + 0.20 * toughness + 0.35 * attack);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
