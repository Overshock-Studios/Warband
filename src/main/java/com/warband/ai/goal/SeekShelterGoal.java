package com.warband.ai.goal;

import com.warband.config.WarbandConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Sun-sensitive undead path to the nearest shaded tile at dawn instead of
 * standing in the open until they catch fire. Predictive: triggers on
 * daytime + sky exposure, not on {@code isOnFire}, so mobs start moving
 * before the first burn tick.
 */
public final class SeekShelterGoal extends Goal implements WarbandGoal {

    private static final int SCAN_RADIUS = 10;
    private static final int RECHECK_TICKS = 20;

    private final Mob mob;
    private BlockPos shelter;
    private int recheckCounter;

    public SeekShelterGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!WarbandConfig.seekShelterEnabled) return false;
        if (mob instanceof Husk || mob instanceof WitherSkeleton) return false;
        if (mob.isInWaterOrRain()) return false;
        if (!mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) return false;
        Level level = mob.level();
        if (!level.isBrightOutside()) return false;
        if (!level.canSeeSky(mob.blockPosition())) return false;
        if (--recheckCounter > 0) return shelter != null;
        recheckCounter = RECHECK_TICKS;
        shelter = findShelter(level, mob.blockPosition());
        return shelter != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (shelter == null) return false;
        if (mob.isInWaterOrRain()) return false;
        if (mob.blockPosition().distSqr(shelter) <= 4) return false;
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, 1.3);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        shelter = null;
    }

    private BlockPos findShelter(Level level, BlockPos origin) {
        BlockPos best = null;
        long bestDist = Long.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.canSeeSky(cursor)) continue;
                    if (!level.getBlockState(cursor).isAir()) continue;
                    if (level.getBlockState(cursor.below()).isAir()) continue;
                    long dist = (long) dx * dx + (long) dz * dz + (long) dy * dy * 2;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
