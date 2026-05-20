package com.warband.ai;

import com.warband.compat.IllagerInvasionCompat;
import com.warband.entity.MobData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

/** Suppresses Warband family/squad friendly fire so vanilla retaliation does not fracture squads. */
public final class FriendlyFireHandler {

    private FriendlyFireHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity attacker = source.getEntity();
            if (!(entity instanceof Mob victim) || !(attacker instanceof Mob attackerMob)) return true;
            if (!MobData.isStamped(victim) || !MobData.isStamped(attackerMob)) return true;

            if (sameSquad(victim, attackerMob) || sameWarbandFamily(victim, attackerMob)) {
                victim.setTarget(null);
                attackerMob.setTarget(null);
                return false;
            }
            return true;
        });
    }

    private static boolean sameSquad(Mob a, Mob b) {
        MobData da = MobData.get(a);
        MobData db = MobData.get(b);
        return da.inSquad() && da.squadId() == db.squadId();
    }

    private static boolean sameWarbandFamily(Mob a, Mob b) {
        if (isZombieFamily(a) || isZombieFamily(b)) return isZombieFamily(a) && isZombieFamily(b);
        if (a instanceof AbstractSkeleton || b instanceof AbstractSkeleton) {
            return a instanceof AbstractSkeleton && b instanceof AbstractSkeleton;
        }
        if (a instanceof Spider || b instanceof Spider) return a instanceof Spider && b instanceof Spider;
        if (a instanceof AbstractPiglin || b instanceof AbstractPiglin) {
            return a instanceof AbstractPiglin && b instanceof AbstractPiglin;
        }
        if (a instanceof Hoglin || a instanceof Zoglin || b instanceof Hoglin || b instanceof Zoglin) {
            return (a instanceof Hoglin || a instanceof Zoglin) && (b instanceof Hoglin || b instanceof Zoglin);
        }
        if (a instanceof Slime || a instanceof MagmaCube || b instanceof Slime || b instanceof MagmaCube) {
            return (a instanceof Slime || a instanceof MagmaCube) && (b instanceof Slime || b instanceof MagmaCube);
        }
        if (a instanceof Blaze || b instanceof Blaze) return a instanceof Blaze && b instanceof Blaze;
        if (IllagerInvasionCompat.isIllagerLike(a) || IllagerInvasionCompat.isIllagerLike(b)) {
            return IllagerInvasionCompat.isIllagerLike(a) && IllagerInvasionCompat.isIllagerLike(b);
        }
        return a.getType() == b.getType();
    }

    private static boolean isZombieFamily(LivingEntity entity) {
        return entity instanceof Zombie || entity instanceof Drowned || entity instanceof ZombifiedPiglin;
    }
}
