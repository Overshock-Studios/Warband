package com.warband.mixin;

import com.warband.ai.ArrowPerception;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arrow-miss perception hook. Catches both block hits (missed shots) and
 * entity hits (collateral) so nearby squads investigate the shooter.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "onHitBlock", at = @At("HEAD"))
    private void warband$onHitBlock(BlockHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level() instanceof ServerLevel level) {
            ArrowPerception.onArrowImpact(level, arrow, result.getLocation());
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void warband$onHitEntity(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level() instanceof ServerLevel level) {
            ArrowPerception.onArrowImpact(level, arrow, result.getLocation());
        }
    }
}
