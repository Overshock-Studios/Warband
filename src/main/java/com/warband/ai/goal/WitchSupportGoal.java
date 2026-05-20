package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/** Witches act as rear support: heal and harden nearby squadmates. */
public final class WitchSupportGoal extends SquadGoal {

    public WitchSupportGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (squad.members().size() < 2 || !decisionReady(100)) return false;

        Mob best = null;
        float lowest = 1.0f;
        for (Mob ally : squad.members()) {
            if (ally == mob || !ally.isAlive()) continue;
            float ratio = ally.getHealth() / ally.getMaxHealth();
            if (ratio < lowest && mob.distanceToSqr(ally) < 12.0 * 12.0) {
                best = ally;
                lowest = ratio;
            }
        }

        if (best == null) return false;
        if (lowest < 0.55f) {
            best.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
        } else {
            best.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, true));
        }
        TacticalEffects.signal((ServerLevel) mob.level(), best.position());
        return true;
    }
}
