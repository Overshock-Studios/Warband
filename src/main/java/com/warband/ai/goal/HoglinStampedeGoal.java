package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Hoglins commit as a stampede when they have a clean visible target. */
public final class HoglinStampedeGoal extends SquadGoal {

    public HoglinStampedeGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.35);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(90)) return false;
        double dist = mob.distanceToSqr(target);
        if (dist < 4.0 * 4.0 || dist > 18.0 * 18.0) return false;

        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0, false, true));
        TacticalEffects.signal((ServerLevel) mob.level(), mob);
        return mob.getNavigation().moveTo(target, speed);
    }
}
