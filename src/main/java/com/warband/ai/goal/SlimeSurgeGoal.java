package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Slimes and magma cubes surge when close enough to swamp movement. */
public final class SlimeSurgeGoal extends SquadGoal {

    public SlimeSurgeGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.25);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(70)) return false;
        if (mob.distanceToSqr(target) > 10.0 * 10.0) return false;

        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1, false, true));
        TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        return mob.getNavigation().moveTo(target, speed);
    }
}
