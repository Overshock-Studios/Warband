package com.warband.ai.goal;

import com.warband.ai.TemporaryTacticBlocks;
import com.warband.WarbandMod;
import com.warband.config.WarbandConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;

/** Temporarily freezes nearby water so higher-level strays can reposition over it. */
public final class FrostWalkerGoal extends Goal implements WarbandGoal {

    private static final int COOLDOWN_TICKS = 20;

    private final Mob mob;
    private int nextDecisionTick;
    private BlockPos base;

    public FrostWalkerGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextDecisionTick) return false;

        ServerLevel level = (ServerLevel) mob.level();
        base = mob.blockPosition().below();
        return isWater(level, base)
                || isWater(level, base.north())
                || isWater(level, base.south())
                || isWater(level, base.east())
                || isWater(level, base.west());
    }

    @Override
    public void start() {
        if (base == null) return;
        nextDecisionTick = mob.tickCount + COOLDOWN_TICKS + mob.getRandom().nextInt(COOLDOWN_TICKS);
        ServerLevel level = (ServerLevel) mob.level();
        TemporaryTacticBlocks.freezeWater(level, base, 20 * 10);
        TemporaryTacticBlocks.freezeWater(level, base.north(), 20 * 10);
        TemporaryTacticBlocks.freezeWater(level, base.south(), 20 * 10);
        TemporaryTacticBlocks.freezeWater(level, base.east(), 20 * 10);
        TemporaryTacticBlocks.freezeWater(level, base.west(), 20 * 10);
        if (WarbandConfig.debugTacticLogs) {
            WarbandMod.LOGGER.info("[Warband] {} used FROST_WALKER at {} {} {} target=none",
                    mob.getType().toShortString(),
                    mob.blockPosition().getX(),
                    mob.blockPosition().getY(),
                    mob.blockPosition().getZ());
        }
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER);
    }
}
