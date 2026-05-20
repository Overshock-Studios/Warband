package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.ai.TemporaryTacticBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Endermen disrupt escape routes with teleports and short-lived block placement. */
public final class EndermanDisruptGoal extends SquadGoal {

    public EndermanDisruptGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(100)) return false;
        if (mob.distanceToSqr(target) > 18.0 * 18.0) return false;

        ServerLevel level = (ServerLevel) mob.level();
        BlockPos pos = disruptionPos(target);
        boolean placed = false;
        if (pos != null) {
            placed = TemporaryTacticBlocks.place(level, pos, carriedOrFallback(), 20 * 8);
        }

        if (mob.distanceToSqr(target) > 8.0 * 8.0) {
            Vec3 behind = target.position().subtract(target.getLookAngle().scale(3.0));
            mob.randomTeleport(behind.x, behind.y, behind.z, true);
        }
        if (placed) {
            TacticalEffects.signal(level, mob);
        }
        return placed;
    }

    private BlockPos disruptionPos(LivingEntity target) {
        BlockPos base = target.blockPosition();
        Vec3 look = target.getLookAngle();
        BlockPos front = base.offset((int) Math.signum(look.x), 0, (int) Math.signum(look.z));
        if (mob.level().getBlockState(front).isAir()) return front;
        if (mob.level().getBlockState(base.above()).isAir()) return base.above();
        return null;
    }

    private BlockState carriedOrFallback() {
        if (mob instanceof EnderMan enderMan && enderMan.getCarriedBlock() != null) {
            return enderMan.getCarriedBlock();
        }
        return Blocks.DIRT.defaultBlockState();
    }
}
