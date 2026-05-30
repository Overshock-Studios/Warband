package com.warband.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/**
 * Out-of-combat skeletons climb to a nearby elevation at dusk/night so they
 * fight from high ground when a player wanders into range, instead of being
 * caught at ground level.
 *
 * <p>Picks a candidate by sampling the heightmap at random offsets, keeping the
 * highest one that is meaningfully above the skeleton's current Y. Cheap — a
 * handful of column lookups every few seconds.
 */
public final class SkeletonPerchGoal extends Goal implements WarbandGoal {

    private static final int DECISION_INTERVAL = 20 * 6;
    private static final int SAMPLES = 8;
    private static final int SAMPLE_RADIUS = 14;
    private static final int MIN_ELEVATION_GAIN = 3;

    private final Mob mob;
    private int nextDecisionTick;
    private BlockPos perch;

    public SkeletonPerchGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;
        if (mob.tickCount < nextDecisionTick) return false;
        nextDecisionTick = mob.tickCount + DECISION_INTERVAL + mob.getRandom().nextInt(DECISION_INTERVAL);

        Level level = mob.level();
        if (!(level instanceof ServerLevel server)) return false;
        // Only perch when it's not bright outside (dusk, night, storms, caves).
        // Daytime perching just leaves them burning on a hilltop.
        if (server.isBrightOutside()) return false;

        BlockPos best = null;
        int bestY = mob.blockPosition().getY() + MIN_ELEVATION_GAIN;
        for (int i = 0; i < SAMPLES; i++) {
            int dx = mob.getRandom().nextInt(SAMPLE_RADIUS * 2 + 1) - SAMPLE_RADIUS;
            int dz = mob.getRandom().nextInt(SAMPLE_RADIUS * 2 + 1) - SAMPLE_RADIUS;
            int sx = mob.blockPosition().getX() + dx;
            int sz = mob.blockPosition().getZ() + dz;
            int sy = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            if (sy <= bestY) continue;
            BlockPos candidate = new BlockPos(sx, sy, sz);
            if (!server.canSeeSky(candidate)) continue;
            best = candidate;
            bestY = sy;
        }
        if (best == null) return false;
        perch = best;
        return true;
    }

    @Override
    public void start() {
        if (perch != null) {
            mob.getNavigation().moveTo(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (perch == null || mob.getTarget() != null) return false;
        if (mob.blockPosition().distSqr(perch) <= 4) return false;
        return !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        perch = null;
    }
}
