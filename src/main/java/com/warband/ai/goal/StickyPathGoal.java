package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TemporaryTacticBlocks;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/** Leaves short-lived webbing behind an active spider. */
public final class StickyPathGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 45;

    private BlockPos webPos;

    public StickyPathGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        if (visibleTarget() == null || !cooldownReady()) return false;
        webPos = mob.blockPosition();
        return mob.level().getBlockState(webPos).isAir();
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (webPos != null) {
            if (TemporaryTacticBlocks.place((ServerLevel) mob.level(), webPos, Blocks.COBWEB, 20 * 8)) {
                logTactic(Tactic.STICKY_PATH);
            }
        }
    }
}
