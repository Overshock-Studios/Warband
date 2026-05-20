package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TemporaryTacticBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/** Leaves short-lived webbing behind an active spider. */
public final class StickyPathGoal extends SquadGoal {

    private BlockPos webPos;

    public StickyPathGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (visibleTarget() == null || !decisionReady(45)) return false;
        webPos = mob.blockPosition();
        return mob.level().getBlockState(webPos).isAir();
    }

    @Override
    public void start() {
        if (webPos != null) {
            TemporaryTacticBlocks.place((ServerLevel) mob.level(), webPos, Blocks.COBWEB, 20 * 8);
        }
    }
}
