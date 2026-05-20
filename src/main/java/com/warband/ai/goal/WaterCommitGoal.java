package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * High-level zombies keep pressure in water instead of dropping the chase as
 * soon as terrain gets awkward. Vanilla handles eventual drowned conversion.
 */
public final class WaterCommitGoal extends SquadGoal {

    public WaterCommitGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        if (!mob.isInWater() || !decisionReady(20)) return false;
        LivingEntity target = visibleTarget();
        boolean moving = target != null && mob.getNavigation().moveTo(target, speed);
        if (moving) {
            TacticalEffects.waterCommit((ServerLevel) mob.level(), mob.position());
        }
        return moving;
    }
}
