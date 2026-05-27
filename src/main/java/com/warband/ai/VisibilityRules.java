package com.warband.ai;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Warband-specific perception modifiers for tactical AI decisions. */
public final class VisibilityRules {

    private VisibilityRules() {
    }

    public static boolean canUseTacticalSight(Mob mob, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (!mob.hasLineOfSight(target)) return false;
        if (target.hasEffect(MobEffects.GLOWING)) return true;
        double allowed = tacticalSightRange(mob, target);
        return mob.distanceToSqr(target) <= allowed * allowed;
    }

    private static double tacticalSightRange(Mob mob, LivingEntity target) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        double range = followRange == null ? 16.0 : followRange.getValue();
        if (range <= 0.0) range = 16.0;

        double multiplier = 1.0;
        if (mob.hasEffect(MobEffects.DARKNESS)) multiplier = Math.min(multiplier, 0.20);
        if (mob.hasEffect(MobEffects.BLINDNESS)) multiplier = Math.min(multiplier, 0.34);
        if (target.hasEffect(MobEffects.INVISIBILITY) || target.isInvisible()) multiplier = Math.min(multiplier, 0.25);
        if (target.isCrouching()) multiplier = Math.min(multiplier, 0.50);
        if (target.getBbHeight() < 1.0F) multiplier = Math.min(multiplier, 0.34);
        return Math.max(2.0, range * multiplier);
    }
}
