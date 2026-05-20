package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Phantoms make repeated high-angle harassment passes. */
public final class PhantomHarassGoal extends SquadGoal {

    public PhantomHarassGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.2);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(70)) return false;

        BlockPos pass = target.blockPosition().offset(
                mob.getRandom().nextInt(9) - 4,
                5 + mob.getRandom().nextInt(4),
                mob.getRandom().nextInt(9) - 4);
        boolean moving = moveTo(pass);
        if (moving) {
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
        return moving;
    }
}
