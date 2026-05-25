package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Periodically moves to a side angle around a visible target. */
public final class FlankGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 60;

    private BlockPos flankPos;

    public FlankGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || mob.distanceToSqr(target) < 16.0 || !cooldownReady()) return false;

        Vec3 toMob = mob.position().subtract(target.position()).normalize();
        Vec3 side = new Vec3(-toMob.z, 0.0, toMob.x);
        if (mob.getRandom().nextBoolean()) {
            side = side.scale(-1.0);
        }
        Vec3 dest = target.position().add(side.scale(5.0)).add(toMob.scale(2.0));
        flankPos = BlockPos.containing(dest.x, dest.y, dest.z);
        return true;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (flankPos != null) {
            moveTo(flankPos);
        }
    }
}
