package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Pulls isolated squad members back toward the current squad center. */
public final class RegroupGoal extends SquadGoal {

    public RegroupGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (squad.members().size() < 2 || !decisionReady(40)) return false;
        Vec3 center = squad.center();
        if (mob.position().distanceToSqr(center) < 14.0 * 14.0) return false;
        return moveTo(BlockPos.containing(center.x, center.y, center.z));
    }
}
