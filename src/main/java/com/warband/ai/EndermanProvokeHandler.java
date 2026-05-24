package com.warband.ai;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla endermen just dodge an arrow with a random teleport and stay neutral.
 * Warband punishes the ranged poke: the enderman reappears right behind the
 * shooter and aggros. The damage is still dodged (you're not free-killing it),
 * but now it is upset and within melee range.
 */
public final class EndermanProvokeHandler {

    private EndermanProvokeHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!WarbandConfig.endermanProvokeEnabled) return true;
            if (!(entity instanceof EnderMan enderman)) return true;
            if (!source.is(DamageTypeTags.IS_PROJECTILE)) return true;
            Entity attacker = source.getEntity();
            if (!(attacker instanceof Player player) || !player.isAlive() || player.isSpectator()) return true;

            Vec3 origin = enderman.position();
            // Drop in front of the shooter for the jumpscare; fall back behind
            // them if the front is blocked.
            Vec3 front = player.position().add(player.getLookAngle().scale(3.0));
            boolean teleported = enderman.randomTeleport(front.x, front.y, front.z, true);
            if (!teleported) {
                Vec3 back = player.position().subtract(player.getLookAngle().scale(2.5));
                teleported = enderman.randomTeleport(back.x, back.y, back.z, true);
            }
            if (teleported) {
                enderman.level().playSound(null, origin.x, origin.y, origin.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
                enderman.level().playSound(null, enderman.getX(), enderman.getY(), enderman.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
            }
            enderman.setTarget(player);
            enderman.setBeingStaredAt();
            return false;
        });
    }
}
