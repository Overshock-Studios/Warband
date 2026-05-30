package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Makes smarter creepers approach from side angles instead of beelining. */
public final class CreeperStalkGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 70;

    private BlockPos stalkPos;

    public CreeperStalkGoal(Mob mob, Squad squad) {
        super(mob, squad, 0.95);
    }

    @Override
    public boolean canUse() {
        if (!cooldownReady()) return false;
        LivingEntity target = visibleTarget();
        Vec3 focus;
        if (target != null) {
            double distance = mob.distanceToSqr(target);
            if (distance < 5.0 * 5.0 || distance > 18.0 * 18.0) return false;
            focus = target.position();
        } else {
            // Pre-stage: hold a flanking position near the squad's last-known
            // contact, so the creeper is already in position when the target
            // returns instead of starting the approach from scratch.
            BlockPos lastKnown = squad.lastKnownPos();
            if (lastKnown == null) return false;
            if (mob.distanceToSqr(lastKnown.getCenter()) > 24.0 * 24.0) return false;
            focus = lastKnown.getCenter();
        }

        Vec3 toMob = mob.position().subtract(focus).normalize();
        Vec3 side = new Vec3(-toMob.z, 0.0, toMob.x);
        if (mob.getRandom().nextBoolean()) {
            side = side.scale(-1.0);
        }
        Vec3 dest = focus.add(side.scale(4.0)).add(toMob.scale(3.0));
        stalkPos = BlockPos.containing(dest.x, dest.y, dest.z);
        return true;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (stalkPos != null && moveTo(stalkPos)) {
            logTactic(Tactic.CREEPER_STALK);
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
    }
}
