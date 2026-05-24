package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.entity.MobData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Keeps squad members from committing to friendly-fire targets and shares a
 * legitimately visible squad target without granting x-ray knowledge.
 */
public final class SquadTargetGoal extends Goal implements WarbandGoal {

    private final Mob mob;
    private final Squad squad;

    public SquadTargetGoal(Mob mob, Squad squad) {
        this.mob = mob;
        this.squad = squad;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return isSquadmate(target) || shouldAdoptVisibleSquadTarget(target);
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (isSquadmate(target)) {
            mob.setTarget(null);
            return;
        }

        LivingEntity squadTarget = squad.target();
        if (squadTarget != null && squadTarget.isAlive() && mob.hasLineOfSight(squadTarget)
                && !shouldRespectEyeContact(squadTarget)) {
            mob.setTarget(squadTarget);
        }
    }

    private boolean shouldAdoptVisibleSquadTarget(LivingEntity currentTarget) {
        LivingEntity squadTarget = squad.target();
        return currentTarget == null
                && squadTarget != null
                && squadTarget.isAlive()
                && mob.hasLineOfSight(squadTarget)
                && !shouldRespectEyeContact(squadTarget);
    }

    /** Endermen only aggro on players who look them in the eye, even via a squad. */
    private boolean shouldRespectEyeContact(LivingEntity target) {
        return mob instanceof EnderMan && target instanceof Player;
    }

    private boolean isSquadmate(LivingEntity target) {
        if (!(target instanceof Mob targetMob)) return false;
        MobData self = MobData.get(mob);
        MobData other = MobData.get(targetMob);
        return self.inSquad() && self.squadId() == other.squadId();
    }
}
