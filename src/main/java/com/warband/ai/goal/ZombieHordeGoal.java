package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Zombie-family mobs try to surround rather than duel in a straight line. */
public final class ZombieHordeGoal extends SquadGoal {

    public ZombieHordeGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !decisionReady(45)) return false;

        double distance = mob.distanceToSqr(target);
        if (distance < 3.0 * 3.0 || distance > 16.0 * 16.0) return false;

        Vec3 toMob = mob.position().subtract(target.position()).normalize();
        Vec3 side = new Vec3(-toMob.z, 0.0, toMob.x);
        if ((mob.getId() & 1) == 0) {
            side = side.scale(-1.0);
        }
        Vec3 dest = target.position().add(side.scale(3.5)).add(toMob.scale(2.0));
        boolean moving = moveTo(BlockPos.containing(dest.x, dest.y, dest.z));
        if (moving && squad.members().size() > 2) {
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
        return moving;
    }
}
