package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TemporaryTacticBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/** Throws a short-lived cobweb at a visible nearby player. */
public final class SpiderWebGoal extends SquadGoal {

    private BlockPos webPos;

    public SpiderWebGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null) return false;
        double distance = mob.distanceToSqr(target);
        if (distance < 2.0 * 2.0 || distance > 8.0 * 8.0) return false;
        if (!decisionReady(100)) return false;

        webPos = target.blockPosition();
        return mob.level().getBlockState(webPos).isAir();
    }

    @Override
    public void start() {
        if (webPos != null) {
            TemporaryTacticBlocks.place((ServerLevel) mob.level(), webPos, Blocks.COBWEB, 20 * 12);
        }
    }
}
