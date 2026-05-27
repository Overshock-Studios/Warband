package com.warband.ai.goal;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Mark of debt: every few seconds, re-apply Glowing to the hunter's target. */
public final class BountyMarkGoal extends Goal {

    private static final int REFRESH_TICKS = 20 * 5;
    private static final int DURATION_TICKS = 20 * 7;

    private final Mob mob;
    private int nextRefreshTick;

    public BountyMarkGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return mob.tickCount >= nextRefreshTick && mob.getTarget() instanceof Player;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        if (mob.getTarget() instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION_TICKS, 0, false, false));
        }
        nextRefreshTick = mob.tickCount + REFRESH_TICKS;
    }
}
