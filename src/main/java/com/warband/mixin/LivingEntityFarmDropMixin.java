package com.warband.mixin;

import com.warband.spawn.AntiFarmDirector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes loot/XP from mobs that Warband identified as farmed. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFarmDropMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void warband$suppressFarmLoot(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof Mob mob && AntiFarmDirector.isFarmSuppressed(mob)) {
            ci.cancel();
        }
    }

    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void warband$suppressFarmExperience(ServerLevel level, net.minecraft.world.entity.Entity killer,
                                                CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Mob mob && AntiFarmDirector.isFarmSuppressed(mob)) {
            cir.setReturnValue(0);
        }
    }
}
