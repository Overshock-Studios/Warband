package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Moves toward the blackboard's decaying last-known position after LOS loss. */
public final class InvestigateLastKnownGoal extends SquadGoal {

    public InvestigateLastKnownGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (visibleTarget() != null || !decisionReady(30)) return false;

        BlockPos lastKnown = squad.lastKnownPos();
        if (lastKnown == null || mob.blockPosition().distSqr(lastKnown) < 4.0) return false;
        boolean moving = moveTo(lastKnown);
        if (moving) {
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
        return moving;
    }
}
