package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Blazes keep vertical fire pressure instead of letting melee pin them. */
public final class BlazeHoverGoal extends SquadGoal {

    public BlazeHoverGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(55)) return false;

        double distance = mob.distanceToSqr(target);
        if (distance > 14.0 * 14.0) return false;

        BlockPos hover = target.blockPosition().offset(
                mob.getRandom().nextInt(9) - 4,
                4 + mob.getRandom().nextInt(3),
                mob.getRandom().nextInt(9) - 4);
        boolean moving = moveTo(hover);
        if (moving) {
            TacticalEffects.signal((ServerLevel) mob.level(), mob.position());
        }
        return moving;
    }
}
