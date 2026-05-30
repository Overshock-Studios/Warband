package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.entity.Tactic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Zombie-family mobs surround a target by distributing themselves around it
 * by squad index, so members approach from all sides instead of stacking on
 * one flank or beelining in single file.
 */
public final class ZombieHordeGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 45;
    private static final double ENCIRCLE_RADIUS = 4.0;

    private BlockPos surroundPos;

    public ZombieHordeGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.0);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = visibleTarget();
        if (target == null || !cooldownReady()) return false;

        double distance = mob.distanceToSqr(target);
        if (distance < 3.0 * 3.0 || distance > 16.0 * 16.0) return false;

        int index = squad.members().indexOf(mob);
        int total = Math.max(squad.members().size(), 1);
        if (index < 0) index = (int) (mob.getId() & 0x7fffffff) % total;
        double angle = (index * (Math.PI * 2.0)) / total;

        Vec3 offset = new Vec3(Math.cos(angle) * ENCIRCLE_RADIUS, 0.0, Math.sin(angle) * ENCIRCLE_RADIUS);
        Vec3 dest = target.position().add(offset);
        surroundPos = BlockPos.containing(dest.x, dest.y, dest.z);
        return true;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (surroundPos != null && moveTo(surroundPos) && squad.members().size() > 1) {
            logTactic(Tactic.ZOMBIE_HORDE);
            TacticalEffects.search((ServerLevel) mob.level(), mob.position());
        }
    }
}
