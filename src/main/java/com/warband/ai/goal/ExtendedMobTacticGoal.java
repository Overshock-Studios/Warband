package com.warband.ai.goal;

import com.warband.ai.Squad;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.phys.Vec3;

public final class ExtendedMobTacticGoal extends SquadGoal {

    public ExtendedMobTacticGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.15);
    }

    @Override
    public boolean canUse() {
        return visibleTarget() != null && decisionReady(60);
    }

    @Override
    public void start() {
        LivingEntity target = visibleTarget();
        if (target == null || !(mob.level() instanceof ServerLevel level)) return;

        if (mob instanceof Guardian) {
            mob.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, false, true));
            mob.getNavigation().moveTo(target, 1.35);
            mob.playAmbientSound();
            return;
        }

        if (mob instanceof Shulker) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0, false, true));
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHULKER_AMBIENT, SoundSource.HOSTILE, 0.8f, 0.75f);
            return;
        }

        if (mob instanceof Ghast) {
            Vec3 away = mob.position().subtract(target.position()).normalize().scale(0.7).add(0.0, 0.25, 0.0);
            mob.setDeltaMovement(mob.getDeltaMovement().add(away));
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 0.9f, 0.9f);
            return;
        }

        if (mob instanceof CaveSpider && mob.distanceToSqr(target) < 5.0 * 5.0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, true));
            return;
        }

        if (mob instanceof Ravager) {
            Vec3 charge = target.position().subtract(mob.position()).normalize().scale(0.85).add(0.0, 0.12, 0.0);
            mob.setDeltaMovement(mob.getDeltaMovement().add(charge));
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 0.9f);
            return;
        }

        if (mob instanceof Warden) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, mob.distanceToSqr(target) > 144.0 ? 1 : 0, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0, false, true));
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 1.0f, 0.75f);
        }
    }
}
