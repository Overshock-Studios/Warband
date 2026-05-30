package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Phantoms make repeated high-angle harassment passes. With no visible target
 * they instead drift toward the nearest sleeping player within range, so a bed
 * isn't an automatic safety guarantee.
 */
public final class PhantomHarassGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 70;
    private static final double SLEEP_WATCH_RADIUS = 64.0;

    private BlockPos pass;

    public PhantomHarassGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.2);
    }

    @Override
    public boolean canUse() {
        if (!cooldownReady()) return false;
        LivingEntity target = visibleTarget();
        BlockPos focus;
        if (target != null) {
            focus = target.blockPosition();
        } else {
            Player sleeper = findSleepingPlayer();
            if (sleeper == null) return false;
            focus = sleeper.blockPosition();
        }
        pass = focus.offset(
                mob.getRandom().nextInt(9) - 4,
                5 + mob.getRandom().nextInt(4),
                mob.getRandom().nextInt(9) - 4);
        return true;
    }

    private Player findSleepingPlayer() {
        AABB box = AABB.ofSize(mob.position(), SLEEP_WATCH_RADIUS * 2, SLEEP_WATCH_RADIUS, SLEEP_WATCH_RADIUS * 2);
        List<Player> nearby = mob.level().getEntitiesOfClass(Player.class, box, p -> p.isAlive() && p.isSleeping());
        if (nearby.isEmpty()) return null;
        Player best = null;
        double bestSqr = Double.MAX_VALUE;
        for (Player p : nearby) {
            double d = mob.distanceToSqr(p);
            if (d < bestSqr) { bestSqr = d; best = p; }
        }
        return best;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (pass != null && moveTo(pass)) {
            logTactic(Tactic.PHANTOM_HARASS);
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
    }
}
