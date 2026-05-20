package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Pulls a badly wounded mob back from the visible threat or last-known position. */
public final class RetreatWhenLowGoal extends SquadGoal {

    public RetreatWhenLowGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.25);
    }

    @Override
    public boolean canUse() {
        if (mob.getHealth() / mob.getMaxHealth() > 0.35f) return false;
        if (!decisionReady(20)) return false;

        LivingEntity target = visibleTarget();
        BlockPos threat = target != null ? target.blockPosition() : squad.lastKnownPos();
        if (threat == null) return false;

        BlockPos retreat = awayFrom(threat.getCenter(), 10.0);
        return retreat != null && moveTo(retreat);
    }
}
