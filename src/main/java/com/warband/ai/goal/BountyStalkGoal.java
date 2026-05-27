package com.warband.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * If the hunter has been kept far from the target for too long, they "find a
 * shortcut": short delay, puff of smoke, reappear a few blocks behind the
 * target. The you-can't-run beat from a stalker enemy.
 */
public final class BountyStalkGoal extends Goal {

    private static final int LOST_AT_DISTANCE_TICKS = 20 * 25;
    private static final double LOST_DISTANCE_SQR = 48.0 * 48.0;
    private static final int COOLDOWN_TICKS = 20 * 45;

    private final Mob mob;
    private int lostTicks;
    private int nextStalkTick;

    public BountyStalkGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextStalkTick) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (mob.distanceToSqr(target) < LOST_DISTANCE_SQR) {
            lostTicks = 0;
            return false;
        }
        lostTicks++;
        return lostTicks >= LOST_AT_DISTANCE_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        ServerLevel level = (ServerLevel) mob.level();
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        Vec3 behind = target.position().subtract(target.getLookAngle().scale(3.5));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(behind.x), (int) Math.floor(behind.z));
        BlockPos dest = new BlockPos((int) Math.floor(behind.x), y, (int) Math.floor(behind.z));

        Vec3 from = mob.position();
        level.playSound(null, from.x, from.y, from.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2f, 0.7f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y + 1.0, from.z, 40, 0.5, 1.0, 0.5, 0.04);

        mob.snapTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, mob.getYRot(), mob.getXRot());

        level.playSound(null, dest.getX(), dest.getY(), dest.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.6f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5, 60, 0.5, 1.0, 0.5, 0.04);

        lostTicks = 0;
        nextStalkTick = mob.tickCount + COOLDOWN_TICKS;
    }
}
