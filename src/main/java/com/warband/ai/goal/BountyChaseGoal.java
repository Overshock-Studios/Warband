package com.warband.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Bounty hunters never lose their target — periodically re-issue a navigation
 * path toward the player so they actively close even without line of sight,
 * even with vanilla goals that would otherwise stand still until the target
 * is visible.
 */
public final class BountyChaseGoal extends Goal {

    private static final int REPATH_TICKS = 30;

    private final Mob mob;
    private final double speed;
    private int nextRepathTick;

    public BountyChaseGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        // No flags: this is a path nudge, not a movement claim. Lets vanilla
        // melee/ranged goals own combat while we keep the navigation honest.
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (mob.tickCount < nextRepathTick && !mob.getNavigation().isDone()) return;
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getNavigation().moveTo(target, speed);
        nextRepathTick = mob.tickCount + REPATH_TICKS;
    }
}
