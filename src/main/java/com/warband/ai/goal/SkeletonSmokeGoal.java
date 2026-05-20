package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Ranged skeleton-family mobs make a visible escape move when crowded. */
public final class SkeletonSmokeGoal extends SquadGoal {

    public SkeletonSmokeGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.2);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || mob.distanceToSqr(target) > 6.0 * 6.0 || !decisionReady(90)) return false;

        BlockPos retreat = awayFrom(target.position(), 8.0);
        if (retreat == null || !moveTo(retreat)) return false;
        TacticalEffects.smoke((ServerLevel) mob.level(), mob.position());
        return true;
    }
}
