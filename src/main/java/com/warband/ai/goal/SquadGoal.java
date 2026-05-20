package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

abstract class SquadGoal extends Goal implements WarbandGoal {

    protected final Mob mob;
    protected final Squad squad;
    protected final double speed;
    private int nextDecisionTick;

    SquadGoal(Mob mob, Squad squad, double speed) {
        this.mob = mob;
        this.squad = squad;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !mob.getNavigation().isDone();
    }

    protected boolean decisionReady(int interval) {
        if (mob.tickCount < nextDecisionTick) return false;
        nextDecisionTick = mob.tickCount + interval + mob.getRandom().nextInt(interval + 1);
        return true;
    }

    protected @Nullable LivingEntity visibleTarget() {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive() && mob.hasLineOfSight(target)) {
            return target;
        }
        return squad.target();
    }

    protected boolean moveTo(BlockPos pos) {
        return mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
    }

    protected @Nullable BlockPos awayFrom(Vec3 threat, double distance) {
        Vec3 current = mob.position();
        Vec3 delta = current.subtract(threat);
        if (delta.lengthSqr() < 0.001) return null;
        Vec3 dest = current.add(delta.normalize().scale(distance));
        return BlockPos.containing(dest.x, dest.y, dest.z);
    }
}
