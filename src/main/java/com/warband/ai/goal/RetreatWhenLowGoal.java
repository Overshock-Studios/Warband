package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.compat.RaidCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Pulls a mob back from a threat when it is badly wounded or its squad is
 * routing (the leader just fell).
 *
 * <p>Retreat is leashed: after {@link #LEASH_TICKS} of continuous fleeing the
 * mob commits and fights for {@link #COMMIT_TICKS}, so a wounded mob cannot flee
 * forever, the player still gets the kill.
 */
public final class RetreatWhenLowGoal extends SquadGoal {

    /** Longest a mob flees continuously before it is forced to commit. */
    private static final int LEASH_TICKS = 20 * 6;
    /** How long the mob then stands and fights before it may flee again. */
    private static final int COMMIT_TICKS = 20 * 5;

    private int retreatingSince = -1;
    private int commitUntil;

    public RetreatWhenLowGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.25);
    }

    @Override
    public boolean canUse() {
        if (RaidCompat.isActiveRaider(mob)) return false;

        boolean wounded = mob.getHealth() / mob.getMaxHealth() <= 0.35f;
        if (!wounded && !squad.isRouting()) {
            retreatingSince = -1;
            return false;
        }
        if (mob.tickCount < commitUntil) return false; // committed, stand and fight
        if (!decisionReady(20)) return false;

        if (retreatingSince < 0) {
            retreatingSince = mob.tickCount;
        }
        if (mob.tickCount - retreatingSince > LEASH_TICKS) {
            // Fled long enough, commit, so the mob cannot kite away forever.
            commitUntil = mob.tickCount + COMMIT_TICKS;
            retreatingSince = -1;
            return false;
        }

        LivingEntity target = visibleTarget();
        BlockPos threat = target != null ? target.blockPosition() : squad.lastKnownPos();
        if (threat == null) return false;

        BlockPos retreat = awayFrom(threat.getCenter(), 10.0);
        return retreat != null && moveTo(retreat);
    }
}
