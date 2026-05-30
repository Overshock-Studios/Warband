package com.warband.ai.goal;

import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Out-of-combat smart mobs opportunistically mount a suitable wild animal,
 * forming jockeys at runtime. Baby zombies hop on chickens; skeletons (other
 * than wither skeletons) hop on spiders. Gated by tactical-difficulty so only
 * "smart enough" mobs do it.
 */
public final class MountJockeyGoal extends Goal implements WarbandGoal {

    private static final int DECISION_INTERVAL = 20 * 5;
    private static final double SCAN_RADIUS = 8.0;
    private static final double MOUNT_RADIUS_SQR = 1.5 * 1.5;

    private final Mob mob;
    private final Class<? extends Mob> mountType;
    private Mob mountTarget;
    private int nextDecisionTick;

    public MountJockeyGoal(Mob mob, Class<? extends Mob> mountType) {
        this.mob = mob;
        this.mountType = mountType;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public static boolean isEligibleRider(Mob mob) {
        if (mob instanceof AbstractSkeleton && !(mob instanceof WitherSkeleton)) return true;
        if (mob instanceof Zombie zombie && zombie.isBaby() && !(zombie instanceof ZombifiedPiglin)) return true;
        return false;
    }

    public static Class<? extends Mob> mountTypeFor(Mob mob) {
        if (mob instanceof AbstractSkeleton) return Spider.class;
        return Chicken.class;
    }

    @Override
    public boolean canUse() {
        if (!WarbandConfig.naturalJockeysEnabled) return false;
        if (mob.isPassenger() || mob.isVehicle()) return false;
        if (mob.getTarget() != null) return false;
        if (MobData.get(mob).difficulty() < WarbandConfig.naturalJockeyMinDifficulty) return false;
        if (mob.tickCount < nextDecisionTick) return false;
        nextDecisionTick = mob.tickCount + DECISION_INTERVAL + mob.getRandom().nextInt(DECISION_INTERVAL);

        AABB box = AABB.ofSize(mob.position(), SCAN_RADIUS * 2, SCAN_RADIUS, SCAN_RADIUS * 2);
        List<? extends Mob> candidates = mob.level().getEntitiesOfClass(mountType, box,
                m -> m.isAlive() && !m.isVehicle() && !m.isPassenger());
        if (candidates.isEmpty()) return false;

        Mob nearest = candidates.getFirst();
        double bestSqr = mob.distanceToSqr(nearest);
        for (Mob c : candidates) {
            double d = mob.distanceToSqr(c);
            if (d < bestSqr) { bestSqr = d; nearest = c; }
        }
        mountTarget = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mountTarget == null || !mountTarget.isAlive()) return false;
        if (mob.getTarget() != null) return false;
        if (mob.isPassenger()) return false;
        return true;
    }

    @Override
    public void tick() {
        if (mountTarget == null) return;
        if (mob.distanceToSqr(mountTarget) <= MOUNT_RADIUS_SQR) {
            mob.startRiding(mountTarget);
            mountTarget = null;
            return;
        }
        mob.getNavigation().moveTo(mountTarget, 1.1);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        mountTarget = null;
    }
}
