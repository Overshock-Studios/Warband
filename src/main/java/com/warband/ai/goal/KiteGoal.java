package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Keeps ranged and skirmisher mobs from standing in easy melee range. */
public final class KiteGoal extends SquadGoal {

    private static final double MIN_DISTANCE_SQR = 7.0 * 7.0;

    public KiteGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.15);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || mob.distanceToSqr(target) > MIN_DISTANCE_SQR) return false;
        if (!decisionReady(15)) return false;

        BlockPos retreat = awayFrom(target.position(), 7.0);
        return retreat != null && moveTo(retreat);
    }
}
