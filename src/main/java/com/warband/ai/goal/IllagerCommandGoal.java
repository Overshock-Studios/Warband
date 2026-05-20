package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/** Illagers behave like disciplined raiders, rallying when the squad holds. */
public final class IllagerCommandGoal extends SquadGoal {

    public IllagerCommandGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (visibleTarget() == null || squad.members().size() < 2 || !decisionReady(90)) return false;

        for (Mob ally : squad.members()) {
            if (ally.isAlive() && mob.distanceToSqr(ally) < 14.0 * 14.0) {
                ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, true));
            }
        }
        TacticalEffects.signal((ServerLevel) mob.level(), squad.center());
        return true;
    }
}
