package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Requests limited reinforcements when a squad is under pressure. */
public final class CallBackupGoal extends SquadGoal {

    public CallBackupGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (!squad.canCallBackup() || visibleTarget() == null || !decisionReady(40)) return false;
        return mob.getHealth() / mob.getMaxHealth() <= 0.45f || squad.morale() < 0.45f;
    }

    @Override
    public void start() {
        if (SquadCoordinator.callBackup(squad, mob.blockPosition())) {
            TacticalEffects.signal((ServerLevel) mob.level(), mob.position());
            squad.markBackupCalled();
        }
    }
}
