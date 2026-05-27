package com.warband.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Bounty-hunter melee: takes over when target is within sword reach. */
public final class BountyMeleeGoal extends MeleeAttackGoal {

    private static final double ENGAGE_RANGE_SQR = 4.0 * 4.0;
    private static final double DISENGAGE_RANGE_SQR = 6.0 * 6.0;

    public BountyMeleeGoal(PathfinderMob mob) {
        super(mob, 1.3, true);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null) return false;
        if (mob.distanceToSqr(target) > ENGAGE_RANGE_SQR) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null) return false;
        if (mob.distanceToSqr(target) > DISENGAGE_RANGE_SQR) return false;
        return super.canContinueToUse();
    }
}
