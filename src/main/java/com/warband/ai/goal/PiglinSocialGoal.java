package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/** Piglins grow bold in groups and panic/regroup when morale collapses. */
public final class PiglinSocialGoal extends SquadGoal {

    public PiglinSocialGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.15);
    }

    @Override
    public boolean canUse() {
        if (!decisionReady(60)) return false;

        if (squad.morale() < 0.45f) {
            BlockPos regroup = BlockPos.containing(squad.center().x, squad.center().y, squad.center().z);
            boolean moving = moveTo(regroup);
            if (moving) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, true));
                TacticalEffects.signal((ServerLevel) mob.level(), mob);
            }
            return moving;
        }

        if (squad.members().size() >= 3 && visibleTarget() != null) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, false, true));
            TacticalEffects.signal((ServerLevel) mob.level(), mob);
            return true;
        }
        return false;
    }
}
