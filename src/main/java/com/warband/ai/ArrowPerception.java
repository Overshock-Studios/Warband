package com.warband.ai;

import com.warband.entity.MobData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Arrow-miss perception: when a player's arrow strikes near a squad, the squad
 * learns roughly where the shot came from and investigates that position.
 * Keeps perception cue-based rather than omniscient: only fires on actual
 * impacts, only when a player owns the arrow, and only for in-squad mobs.
 */
public final class ArrowPerception {

    private static final double ALERT_RADIUS = 12.0;

    private ArrowPerception() {
    }

    public static void onArrowImpact(ServerLevel level, AbstractArrow arrow, Vec3 impact) {
        Entity owner = arrow.getOwner();
        if (!(owner instanceof Player shooter) || !shooter.isAlive() || shooter.isSpectator()) return;
        BlockPos shotFrom = shooter.blockPosition();

        AABB box = AABB.ofSize(impact, ALERT_RADIUS * 2.0, ALERT_RADIUS, ALERT_RADIUS * 2.0);
        Set<Integer> alertedSquads = new HashSet<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box,
                m -> m.isAlive() && MobData.isStamped(m))) {
            MobData data = MobData.get(mob);
            if (data.inSquad()) {
                if (!alertedSquads.add(data.squadId())) continue;
                Squad squad = SquadCoordinator.getSquad(data.squadId());
                if (squad != null) squad.alertTo(shotFrom);
            } else {
                alertSolo(mob, shooter, data.difficulty());
            }
        }
    }

    /**
     * Solo Warband mobs have no shared blackboard, so we alert by acquiring the
     * shooter as target directly. Difficulty-gated: a dumb mob barely notices
     * (~10% at diff 0.2), a smart one usually does (~70% at diff 1.0). Never
     * overrides an existing target.
     */
    private static void alertSolo(Mob mob, Player shooter, float difficulty) {
        if (mob.getTarget() != null) return;
        // Endermen still demand eye contact; a missed arrow doesn't provoke them.
        if (mob instanceof EnderMan) return;
        if (difficulty < 0.20f) return;
        if (mob.getRandom().nextDouble() >= difficulty * 0.7) return;
        mob.setTarget(shooter);
    }
}
