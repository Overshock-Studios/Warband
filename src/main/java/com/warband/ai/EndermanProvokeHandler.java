package com.warband.ai;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
            Player player = projectileOwner(source.getEntity(), source.getDirectEntity());
            if (player == null || !player.isAlive() || player.isSpectator()) return true;

            Vec3 origin = enderman.position();
            boolean teleported = teleportNearShooter(enderman, player);
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

    private static Player projectileOwner(Entity sourceEntity, Entity directEntity) {
        if (sourceEntity instanceof Player player) return player;
        if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static boolean teleportNearShooter(EnderMan enderman, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3[] attempts = {
                player.position().add(look.scale(3.0)),
                player.position().subtract(look.scale(2.5)),
                player.position().add(look.yRot(1.5707964F).scale(2.5)),
                player.position().add(look.yRot(-1.5707964F).scale(2.5)),
                player.position().add(look.scale(4.5)),
                player.position().subtract(look.scale(4.0))
        };
        for (Vec3 attempt : attempts) {
            if (enderman.randomTeleport(attempt.x, attempt.y, attempt.z, true)) {
                return true;
            }
        }
        return false;
    }
}
