package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Pulls isolated squad members back toward the current squad center. */
public final class RegroupGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 40;

    private BlockPos regroupPos;

    public RegroupGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (squad.members().size() < 2 || !cooldownReady()) return false;
        Vec3 center = squad.center();
        if (mob.position().distanceToSqr(center) < 14.0 * 14.0) return false;
        regroupPos = BlockPos.containing(center.x, center.y, center.z);
        return true;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (regroupPos != null) {
            moveTo(regroupPos);
        }
    }
}
