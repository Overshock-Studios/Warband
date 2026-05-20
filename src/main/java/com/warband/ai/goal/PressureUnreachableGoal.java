package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TacticalEffects;
import com.warband.entity.MobData;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Searches around the last-known position when direct pathing stalls. */
public final class PressureUnreachableGoal extends SquadGoal {

    public PressureUnreachableGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        if (!decisionReady(80)) return false;

        LivingEntity target = visibleTarget();
        BlockPos pressurePoint = target != null ? target.blockPosition() : squad.lastKnownPos();
        if (pressurePoint == null) return false;
        if (mob.distanceToSqr(pressurePoint.getCenter()) < 8.0 * 8.0) return false;

        boolean stalled = mob.getNavigation().isDone() || mob.getNavigation().isStuck();
        if (!stalled) return false;

        MobData data = MobData.get(mob);
        if (target != null && data.hasTactic(Tactic.LEAP_UNREACHABLE) && tryLeap(target)) {
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
            return true;
        }
        if (target != null && data.hasTactic(Tactic.MOB_STACK_CLIMB) && tryStackClimb(target)) {
            TacticalEffects.signal((ServerLevel) mob.level(), mob);
            return true;
        }

        Vec3 offset = new Vec3(mob.getRandom().nextInt(9) - 4, 0, mob.getRandom().nextInt(9) - 4);
        BlockPos searchPos = BlockPos.containing(pressurePoint.getCenter().add(offset));
        if (!moveTo(searchPos)) return false;

        TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        if (squad.canCallBackup()) {
            SquadCoordinator.callBackup(squad, mob.blockPosition());
            squad.markBackupCalled();
        }
        return true;
    }

    private boolean tryLeap(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(mob.position());
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horizontal < 2.0 || horizontal > 12.0 || target.getY() < mob.getY() + 1.0) return false;

        Vec3 direction = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
        double lift = Math.min(0.85, 0.35 + (target.getY() - mob.getY()) * 0.12);
        mob.setDeltaMovement(mob.getDeltaMovement().add(direction.scale(0.7)).add(0.0, lift, 0.0));
        return true;
    }

    private boolean tryStackClimb(LivingEntity target) {
        if (target.getY() < mob.getY() + 1.5 || mob.distanceToSqr(target) > 10.0 * 10.0) return false;

        AABB box = AABB.ofSize(mob.position(), 7.0, 3.0, 7.0);
        List<Mob> allies = ((ServerLevel) mob.level()).getEntitiesOfClass(Mob.class, box, ally ->
                ally != mob
                        && ally.isAlive()
                        && MobData.get(ally).squadId() == MobData.get(mob).squadId()
                        && ally.distanceToSqr(mob) < 4.0 * 4.0);
        if (allies.isEmpty()) return false;

        Mob ally = allies.get(mob.getRandom().nextInt(allies.size()));
        Vec3 top = ally.position().add(0.0, ally.getBbHeight() + 0.05, 0.0);
        if (mob.position().distanceToSqr(top) > 1.8 * 1.8) {
            return mob.getNavigation().moveTo(top.x, top.y, top.z, 1.2);
        }

        Vec3 toTarget = target.position().subtract(mob.position());
        Vec3 direction = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (direction.lengthSqr() > 0.001) {
            direction = direction.normalize().scale(0.35);
        }
        mob.setDeltaMovement(mob.getDeltaMovement().add(direction).add(0.0, 0.55, 0.0));
        return true;
    }
}
