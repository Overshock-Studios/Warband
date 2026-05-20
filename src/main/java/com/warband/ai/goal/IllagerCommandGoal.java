package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TacticalEffects;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Illagers behave like disciplined raiders, turning squad losses into pressure. */
public final class IllagerCommandGoal extends SquadGoal {

    public IllagerCommandGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || squad.members().size() < 2 || !decisionReady(70)) return false;

        boolean leader = MobData.get(mob).role() == Role.LEADER;
        boolean vengeful = squad.morale() < 0.60f;
        for (Mob ally : squad.members()) {
            if (!ally.isAlive() || mob.distanceToSqr(ally) >= 18.0 * 18.0) continue;
            ally.setTarget(target);
            ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 90, vengeful ? 1 : 0, false, true));
            if (leader || vengeful) {
                ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 90, 0, false, true));
            }
        }
        if (vengeful && squad.canCallBackup()) {
            SquadCoordinator.callBackup(squad, mob.blockPosition());
            squad.markBackupCalled();
        }
        if (leader) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
        }
        TacticalEffects.signal((ServerLevel) mob.level(), squad.center());
        return true;
    }
}
