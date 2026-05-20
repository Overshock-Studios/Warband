package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Searches around the last-known position when direct pathing stalls. */
public final class PressureUnreachableGoal extends SquadGoal {

    public PressureUnreachableGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        if (!decisionReady(80)) return false;

        LivingEntity target = visibleTarget();
        BlockPos pressurePoint = target != null ? target.blockPosition() : squad.lastKnownPos();
        if (pressurePoint == null) return false;
        if (mob.distanceToSqr(pressurePoint.getCenter()) < 8.0 * 8.0) return false;

        boolean stalled = mob.getNavigation().isDone() || mob.getNavigation().isStuck();
        if (!stalled) return false;

        Vec3 offset = new Vec3(mob.getRandom().nextInt(9) - 4, 0, mob.getRandom().nextInt(9) - 4);
        BlockPos searchPos = BlockPos.containing(pressurePoint.getCenter().add(offset));
        if (!moveTo(searchPos)) return false;

        TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        if (squad.canCallBackup()) {
            SquadCoordinator.callBackup(squad, mob.blockPosition());
            squad.markBackupCalled();
        }
        return true;
    }
}
