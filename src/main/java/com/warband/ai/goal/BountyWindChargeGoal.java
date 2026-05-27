package com.warband.ai.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Bounty hunter mobility burst. This imitates using a wind charge for movement
 * without adding a damaging projectile around the player.
 */
public final class BountyWindChargeGoal extends Goal {

    private static final int COOLDOWN_TICKS = 20 * 5;
    private static final double MIN_VERTICAL = 1.75;
    private static final double MAX_VERTICAL = 8.0;
    private static final double MAX_HORIZONTAL = 14.0;

    private final Mob mob;
    private int nextUseTick;

    public BountyWindChargeGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextUseTick) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dy = target.getY() - mob.getY();
        if (dy < MIN_VERTICAL || dy > MAX_VERTICAL) return false;
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        if (dx * dx + dz * dz > MAX_HORIZONTAL * MAX_HORIZONTAL) return false;
        return mob.onGround() && (mob.getNavigation().isDone() || mob.getNavigation().isStuck());
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        Vec3 toward = target.position().subtract(mob.position());
        Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
        if (horizontal.lengthSqr() > 0.001) {
            horizontal = horizontal.normalize().scale(0.45);
        }
        double vertical = Math.min(0.95, 0.45 + Math.max(0.0, target.getY() - mob.getY()) * 0.12);
        mob.setDeltaMovement(mob.getDeltaMovement().add(horizontal.x, vertical, horizontal.z));
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 0.8f, 0.85f);
        nextUseTick = mob.tickCount + COOLDOWN_TICKS;
    }
}
