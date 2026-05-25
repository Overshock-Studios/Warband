package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Searches nearby lateral positions and prefers one that breaks player sight. */
public final class BreakLosGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 50;

    private BlockPos losBreakPos;

    public BreakLosGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.2);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || mob.distanceToSqr(target) > 12.0 * 12.0 || !cooldownReady()) return false;

        Vec3 away = mob.position().subtract(target.position()).normalize();
        Vec3 side = new Vec3(-away.z, 0.0, away.x);
        for (int i = 0; i < 6; i++) {
            double sign = (i & 1) == 0 ? 1.0 : -1.0;
            double depth = 3.0 + i;
            Vec3 dest = mob.position().add(away.scale(depth)).add(side.scale(sign * 4.0));
            BlockPos pos = BlockPos.containing(dest.x, dest.y, dest.z);
            losBreakPos = pos;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (losBreakPos != null) {
            moveTo(losBreakPos);
        }
    }
}
