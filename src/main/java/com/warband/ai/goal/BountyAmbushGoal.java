package com.warband.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * If a bounty hunter has closed within a short distance of their target but
 * doesn't have line of sight, they hold position and look around instead of
 * sprinting straight through. Gives the "they were waiting around the corner"
 * beat — the hunter is already where you're about to walk.
 */
public final class BountyAmbushGoal extends Goal {

    private static final double TRIGGER_RANGE_SQR = 12.0 * 12.0;
    private static final int HOLD_TICKS = 20 * 4;
    private static final int COOLDOWN_TICKS = 20 * 15;

    private final Mob mob;
    private int holdUntilTick;
    private int nextAmbushTick;

    public BountyAmbushGoal(Mob mob) {
        this.mob = mob;
        // Claim MOVE so vanilla chase goals don't keep repathing through us
        // while we're laying in wait.
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextAmbushTick) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (mob.distanceToSqr(target) > TRIGGER_RANGE_SQR) return false;
        if (mob.hasLineOfSight(target)) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.tickCount >= holdUntilTick) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Break the ambush the instant they show themselves.
        return !mob.hasLineOfSight(target);
    }

    @Override
    public void start() {
        holdUntilTick = mob.tickCount + HOLD_TICKS;
        nextAmbushTick = mob.tickCount + HOLD_TICKS + COOLDOWN_TICKS;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
