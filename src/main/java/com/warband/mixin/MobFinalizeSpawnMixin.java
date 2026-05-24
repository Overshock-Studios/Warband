package com.warband.mixin;

import com.warband.spawn.SpawnDirector;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks {@link Mob#finalizeSpawn}, the choke point every natural spawn passes
 * through, to hand the mob to the {@link SpawnDirector} for difficulty stamping
 * and stat buffs.
 */
@Mixin(Mob.class)
public abstract class MobFinalizeSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void warband$onFinalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         EntitySpawnReason reason, SpawnGroupData groupData,
                                         CallbackInfoReturnable<SpawnGroupData> cir) {
        SpawnDirector.onMobFinalizeSpawn((Mob) (Object) this, level, reason);
    }
}
