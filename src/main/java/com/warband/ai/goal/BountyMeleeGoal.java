package com.warband.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Bounty-hunter melee: takes over when target is within sword reach. */
public final class BountyMeleeGoal extends MeleeAttackGoal {

    private static final double ENGAGE_RANGE_SQR = 4.0 * 4.0;
    private static final double DISENGAGE_RANGE_SQR = 6.0 * 6.0;

    public BountyMeleeGoal(PathfinderMob mob) {
        super(mob, 1.3, true);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null) return false;
        if (mob.distanceToSqr(target) > ENGAGE_RANGE_SQR) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null) return false;
        if (mob.distanceToSqr(target) > DISENGAGE_RANGE_SQR) return false;
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        equipMeleeMainhand();
        super.start();
    }

    @Override
    public void stop() {
        restoreCrossbowMainhand();
        super.stop();
    }

    private void equipMeleeMainhand() {
        ItemStack main = mob.getMainHandItem();
        ItemStack off = mob.getOffhandItem();
        if (isMeleeWeapon(main) || !isMeleeWeapon(off)) return;
        mob.setItemSlot(EquipmentSlot.MAINHAND, off);
        mob.setItemSlot(EquipmentSlot.OFFHAND, main);
    }

    private void restoreCrossbowMainhand() {
        ItemStack main = mob.getMainHandItem();
        ItemStack off = mob.getOffhandItem();
        if (!main.is(Items.CROSSBOW) && off.is(Items.CROSSBOW)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, off);
            mob.setItemSlot(EquipmentSlot.OFFHAND, main);
        }
    }

    private boolean isMeleeWeapon(ItemStack stack) {
        return stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.IRON_AXE)
                || stack.is(Items.STONE_AXE)
                || stack.is(Items.DIAMOND_AXE);
    }
}
