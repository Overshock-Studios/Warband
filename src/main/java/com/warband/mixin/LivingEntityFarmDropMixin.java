package com.warband.mixin;

import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.illager.IllagerGrudgeSystem;
import com.warband.spawn.AntiFarmDirector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes loot/XP from mobs that Warband identified as farmed. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFarmDropMixin {

    @Shadow protected int deathTime;

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void warband$bountyRevive(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (IllagerGrudgeSystem.tryBountyRevive((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void warband$clearFalseDeathState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getHealth() > 0.0f && !self.isRemoved()) {
            deathTime = 0;
            self.setPose(Pose.STANDING);
            ci.cancel();
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void warband$suppressFarmLoot(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof Mob mob && AntiFarmDirector.isFarmSuppressed(mob)) {
            ci.cancel();
        }
    }

    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void warband$suppressFarmExperience(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Mob mob && AntiFarmDirector.isFarmSuppressed(mob)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void warband$scaleLegitimateWarbandExperience(ServerLevel level, Entity killer,
                                                          CallbackInfoReturnable<Integer> cir) {
        if (!WarbandConfig.experienceScalingEnabled || !(killer instanceof ServerPlayer)) return;
        if (!((Object) this instanceof Mob mob) || AntiFarmDirector.isFarmSuppressed(mob)) return;
        if (!MobData.isStamped(mob)) return;

        int base = cir.getReturnValue();
        if (base <= 0) return;

        MobData data = MobData.get(mob);
        double multiplier = 1.0 + data.difficulty() * WarbandConfig.experienceDifficultyBonusMax;
        if (data.role().isLeader()) {
            multiplier += WarbandConfig.experienceLeaderBonus;
        }
        cir.setReturnValue(Math.max(base, (int) Math.round(base * multiplier)));
    }
}
