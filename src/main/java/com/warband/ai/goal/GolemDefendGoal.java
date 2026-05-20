package com.warband.ai.goal;

import com.warband.ai.TacticalEffects;
import com.warband.entity.MobData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Golems prioritize Warband-stamped threats and help each other hold space. */
public final class GolemDefendGoal extends Goal implements WarbandGoal {

    private static final double THREAT_RADIUS = 32.0;
    private final Mob golem;
    private int nextScanTick;

    public GolemDefendGoal(Mob golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (golem.tickCount < nextScanTick) return false;
        nextScanTick = golem.tickCount + 20 + golem.getRandom().nextInt(20);

        LivingEntity current = golem.getTarget();
        if (current != null && current.isAlive() && MobData.isStamped(current)) {
            return false;
        }

        AABB box = AABB.ofSize(golem.position(), THREAT_RADIUS * 2.0, THREAT_RADIUS, THREAT_RADIUS * 2.0);
        List<Mob> threats = ((ServerLevel) golem.level()).getEntitiesOfClass(Mob.class, box,
                mob -> mob instanceof Enemy && MobData.isStamped(mob) && mob.isAlive());
        threats.stream()
                .min(Comparator.comparingDouble(golem::distanceToSqr))
                .ifPresent(threat -> {
                    golem.setTarget(threat);
                    TacticalEffects.signal((ServerLevel) golem.level(), golem);
                });
        return golem.getTarget() != current;
    }

    @Override
    public void tick() {
        LivingEntity target = golem.getTarget();
        if (target == null || !target.isAlive()) return;

        if (golem instanceof SnowGolem && golem.distanceToSqr(target) < 8.0 * 8.0) {
            var away = golem.position().subtract(target.position());
            if (away.lengthSqr() > 0.001) {
                var dest = golem.position().add(away.normalize().scale(5.0));
                golem.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.15);
            }
        } else {
            golem.getNavigation().moveTo(target, 1.05);
        }
    }
}
