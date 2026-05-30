package com.warband.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Out-of-combat zombies drift toward the nearest other zombie within range,
 * so wanderers naturally cluster into small packs instead of starving alone.
 * Throttled and cheap: a slow scan every few seconds, only when this mob has
 * no target and is idle.
 */
public final class ZombiePackGoal extends Goal implements WarbandGoal {

    private static final int DECISION_INTERVAL = 20 * 5;
    private static final double SEARCH_RADIUS = 16.0;
    private static final double JOIN_RADIUS_SQR = 5.0 * 5.0;

    private final Mob mob;
    private int nextDecisionTick;
    private BlockPos pairPos;

    public ZombiePackGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;
        if (mob.tickCount < nextDecisionTick) return false;
        nextDecisionTick = mob.tickCount + DECISION_INTERVAL + mob.getRandom().nextInt(DECISION_INTERVAL);

        AABB box = AABB.ofSize(mob.position(), SEARCH_RADIUS * 2, SEARCH_RADIUS, SEARCH_RADIUS * 2);
        List<Zombie> nearby = mob.level().getEntitiesOfClass(Zombie.class, box,
                z -> z != mob && z.isAlive() && z.getTarget() == null);
        if (nearby.isEmpty()) return false;

        Zombie nearest = null;
        double bestSqr = Double.MAX_VALUE;
        for (Zombie z : nearby) {
            double d = mob.distanceToSqr(z);
            if (d < bestSqr) { bestSqr = d; nearest = z; }
        }
        // Already paired close enough — no need to drift.
        if (nearest == null || bestSqr <= JOIN_RADIUS_SQR) return false;
        pairPos = nearest.blockPosition();
        return true;
    }

    @Override
    public void start() {
        if (pairPos != null) {
            mob.getNavigation().moveTo(pairPos.getX() + 0.5, pairPos.getY(), pairPos.getZ() + 0.5, 0.85);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (pairPos == null || mob.getTarget() != null) return false;
        if (mob.blockPosition().distSqr(pairPos) <= JOIN_RADIUS_SQR) return false;
        return !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        pairPos = null;
    }
}
