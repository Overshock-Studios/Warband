package com.warband.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

/**
 * Bounty-hunter parkour: when the target is several blocks above the hunter and
 * pathing has stalled, jump straight up and place a cobblestone underneath. Caps
 * total placements per hunter so it does not pillar to the build limit.
 */
public final class BountyClimbGoal extends Goal {

    private static final int COOLDOWN_TICKS = 25;
    private static final int MAX_PILLARS = 12;
    private static final double MIN_VERTICAL = 2.5;
    private static final double MAX_HORIZONTAL = 5.0;

    private final Mob mob;
    private int cooldown;
    private int pillarsBuilt;
    private int activeTicks;
    private double pillarFromY = Double.NaN;

    public BountyClimbGoal(Mob mob) {
        this.mob = mob;
        // No flags: runs alongside attack/move goals, doesn't block them.
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (pillarsBuilt >= MAX_PILLARS) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (target.getY() < mob.getY() + MIN_VERTICAL) return false;
        if (Math.abs(target.getX() - mob.getX()) > MAX_HORIZONTAL) return false;
        if (Math.abs(target.getZ() - mob.getZ()) > MAX_HORIZONTAL) return false;
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!mob.onGround()) return false;
        return mob.getNavigation().isDone() || mob.getNavigation().isStuck();
    }

    @Override
    public boolean canContinueToUse() {
        return !Double.isNaN(pillarFromY) && activeTicks < 30;
    }

    @Override
    public void start() {
        cooldown = COOLDOWN_TICKS;
        pillarFromY = mob.getY();
        activeTicks = 0;
        mob.getJumpControl().jump();
    }

    @Override
    public void tick() {
        activeTicks++;
        if (Double.isNaN(pillarFromY)) return;
        ServerLevel level = (ServerLevel) mob.level();
        // Block goes in once we've cleared a half-block above the starting Y.
        if (mob.getY() > pillarFromY + 0.6) {
            BlockPos placeAt = BlockPos.containing(mob.getX(), pillarFromY, mob.getZ());
            if (level.getBlockState(placeAt).canBeReplaced()) {
                level.setBlockAndUpdate(placeAt, Blocks.COBBLESTONE.defaultBlockState());
                level.playSound(null, placeAt, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 0.7f, 1.0f);
                pillarsBuilt++;
            }
            pillarFromY = Double.NaN;
        }
    }

    @Override
    public void stop() {
        pillarFromY = Double.NaN;
        activeTicks = 0;
    }
}
