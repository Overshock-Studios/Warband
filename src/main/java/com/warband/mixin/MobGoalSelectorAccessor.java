package com.warband.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for injecting Warband tactical goals without widening all of Mob. */
@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {

    @Accessor("goalSelector")
    GoalSelector warband$goalSelector();

    @Accessor("targetSelector")
    GoalSelector warband$targetSelector();
}
