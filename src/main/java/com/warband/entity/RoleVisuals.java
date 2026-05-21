package com.warband.entity;

import com.warband.WarbandMod;
import com.warband.ai.TacticalEffects;
import com.warband.compat.IllagerInvasionCompat;
import com.warband.config.WarbandConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Shared visual language for squad roles on vanilla mobs. */
public final class RoleVisuals {

    private static final Identifier SCALE_MOD = Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "role_scale");

    private RoleVisuals() {
    }

    public static void apply(Mob mob, Role role, double difficulty) {
        if (role == Role.NONE) return;

        applyRoleName(mob, role, difficulty);
        TacticalEffects.roleCue(mob, role);
        if (!WarbandConfig.roleVisualsEnabled) return;
        applyScale(mob, role);
        if (IllagerInvasionCompat.isIllagerLike(mob) || difficulty < 0.35) return;

        switch (role) {
            case LEADER -> {
                equipArmor(mob, EquipmentSlot.HEAD, enchanted(mob, new ItemStack(Items.GOLDEN_HELMET), Enchantments.PROTECTION, 1));
                equipArmor(mob, EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            }
            case BRUISER -> {
                if (mob instanceof Zombie) {
                    replaceWeapon(mob, enchanted(mob, new ItemStack(Items.IRON_AXE), Enchantments.SHARPNESS, level(difficulty, 1, 2)));
                }
                equipArmor(mob, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            case MARKSMAN -> {
                equipArmor(mob, EquipmentSlot.HEAD, enchanted(mob, new ItemStack(Items.LEATHER_HELMET), Enchantments.PROJECTILE_PROTECTION, 1));
                if (mob instanceof RangedAttackMob && !mob.getMainHandItem().isEmpty()) {
                    enchantExistingWeapon(mob, difficulty);
                }
            }
            case SKIRMISHER -> {
                equipArmor(mob, EquipmentSlot.FEET, enchanted(mob, new ItemStack(Items.LEATHER_BOOTS), Enchantments.PROTECTION, 1));
                equipArmor(mob, EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            }
            case SUPPORT -> {
                equipArmor(mob, EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                equipArmor(mob, EquipmentSlot.CHEST, enchanted(mob, new ItemStack(Items.CHAINMAIL_CHESTPLATE), Enchantments.PROTECTION, 1));
            }
            case NONE -> {
            }
        }
    }

    private static void applyRoleName(Mob mob, Role role, double difficulty) {
        if (!WarbandConfig.roleNamesEnabled || mob.hasCustomName() || IllagerInvasionCompat.isIllagerLike(mob)) return;
        if (role != Role.LEADER && difficulty < 0.75) return;
        mob.setCustomName(Component.literal(displayName(role) + " " + mob.getType().getDescription().getString()));
        mob.setCustomNameVisible(true);
    }

    private static String displayName(Role role) {
        return switch (role) {
            case BRUISER -> "Bruiser";
            case SKIRMISHER -> "Flanker";
            case MARKSMAN -> "Marksman";
            case SUPPORT -> "Support";
            case LEADER -> "Leader";
            case NONE -> "";
        };
    }

    private static void applyScale(Mob mob, Role role) {
        AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
        if (scale == null || scale.hasModifier(SCALE_MOD)) return;

        double amount = switch (role) {
            case LEADER -> 0.12;
            case BRUISER -> 0.08;
            case SKIRMISHER -> -0.06;
            case MARKSMAN -> 0.03;
            case SUPPORT -> -0.03;
            case NONE -> 0.0;
        };
        if (amount != 0.0) {
            scale.addPermanentModifier(new AttributeModifier(SCALE_MOD, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void equipArmor(Mob mob, EquipmentSlot slot, ItemStack stack) {
        if (!mob.getItemBySlot(slot).isEmpty()) return;
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.015f);
    }

    private static void replaceWeapon(Mob mob, ItemStack stack) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.02f);
    }

    private static void enchantExistingWeapon(Mob mob, double difficulty) {
        ItemStack weapon = mob.getMainHandItem();
        if (weapon.is(Items.BOW)) {
            enchanted(mob, weapon, Enchantments.POWER, level(difficulty, 1, 2));
        } else if (weapon.is(Items.CROSSBOW)) {
            enchanted(mob, weapon, Enchantments.QUICK_CHARGE, level(difficulty, 1, 2));
        }
    }

    private static ItemStack enchanted(Mob mob, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> enchantment = mob.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        stack.enchant(enchantment, level);
        return stack;
    }

    private static int level(double difficulty, int min, int max) {
        return Math.max(min, Math.min(max, min + (int) Math.floor(difficulty * max)));
    }
}
