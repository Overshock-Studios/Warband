package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.ai.TemporaryTacticBlocks;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Throws a short-lived cobweb at a visible nearby player. */
public final class SpiderWebGoal extends SquadGoal {

    private static final int WINDUP_TICKS = 14;
    private static final int COOLDOWN_TICKS = 100;

    private BlockPos webPos;
    private int fireAtTick;

    public SpiderWebGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null) return false;
        double distance = mob.distanceToSqr(target);
        if (distance < 2.0 * 2.0 || distance > 8.0 * 8.0) return false;
        if (!cooldownReady()) return false;

        webPos = target.blockPosition();
        return mob.level().getBlockState(webPos).isAir();
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        fireAtTick = mob.tickCount + WINDUP_TICKS;
        tick();
    }

    @Override
    public boolean canContinueToUse() {
        return webPos != null && mob.tickCount <= fireAtTick + 1;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (webPos == null) return;

        ServerLevel level = (ServerLevel) mob.level();
        Vec3 from = mob.position().add(0.0, mob.getBbHeight() * 0.65, 0.0);
        Vec3 to = webPos.getCenter();
        TacticalEffects.webTrail(level, from, to);

        if (mob.tickCount < fireAtTick) return;
        if (level.getBlockState(webPos).isAir()) {
            if (TemporaryTacticBlocks.place(level, webPos, Blocks.COBWEB, 20 * 12)) {
                logTactic(Tactic.SPIDER_WEB);
            }
        }
        webPos = null;
    }
}
