package com.warband.entity;

import com.warband.compat.IllagerInvasionCompat;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

/** Difficulty-gated situational tactics assigned when Warband stamps a mob. */
public enum Tactic {

    SPIDER_WEB(1 << 0),
    STICKY_PATH(1 << 1),
    FROST_WALKER(1 << 2),
    WATER_COMMIT(1 << 3),
    PRESSURE_UNREACHABLE(1 << 4),
    SKELETON_SMOKE(1 << 5),
    CREEPER_STALK(1 << 6),
    ZOMBIE_HORDE(1 << 7),
    ENDERMAN_DISRUPT(1 << 8),
    PIGLIN_SOCIAL(1 << 9),
    BLAZE_HOVER(1 << 10),
    WITCH_SUPPORT(1 << 11),
    SLIME_SURGE(1 << 12),
    HOGLIN_STAMPEDE(1 << 13),
    ILLAGER_COMMAND(1 << 14),
    PHANTOM_HARASS(1 << 15),
    LEAP_UNREACHABLE(1 << 16),
    MOB_STACK_CLIMB(1 << 17),
    GUARDIAN_SURGE(1 << 18),
    SHULKER_LOCKDOWN(1 << 19),
    GHAST_REPOSITION(1 << 20),
    CAVE_SPIDER_AMBUSH(1 << 21),
    RAVAGER_BREAKER(1 << 22),
    WARDEN_PRESSURE(1 << 23);

    private final int bit;

    Tactic(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public static boolean has(int mask, Tactic tactic) {
        return (mask & tactic.bit) != 0;
    }

    public static int chooseFor(Mob mob, double difficulty, Role role) {
        int mask = 0;
        if (mob instanceof Spider) {
            if (difficulty >= 0.45) mask |= SPIDER_WEB.bit;
            if (difficulty >= 0.65 || role == Role.SKIRMISHER) mask |= STICKY_PATH.bit;
        }
        if (mob instanceof CaveSpider && difficulty >= 0.45) {
            mask |= CAVE_SPIDER_AMBUSH.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof AbstractSkeleton) {
            if (difficulty >= 0.50) mask |= PRESSURE_UNREACHABLE.bit;
            if (difficulty >= 0.60) mask |= SKELETON_SMOKE.bit;
        }
        if (mob instanceof Stray && difficulty >= 0.55) {
            mask |= FROST_WALKER.bit;
        }
        if (mob instanceof Zombie || mob instanceof Drowned || mob instanceof ZombifiedPiglin) {
            if (difficulty >= 0.40) mask |= ZOMBIE_HORDE.bit;
        }
        if ((mob instanceof Zombie || mob instanceof Drowned || mob instanceof ZombifiedPiglin) && difficulty >= 0.70) {
            mask |= WATER_COMMIT.bit;
        }
        if ((mob instanceof Zombie || mob instanceof Drowned || mob instanceof ZombifiedPiglin) && difficulty >= 0.80) {
            mask |= LEAP_UNREACHABLE.bit;
        }
        if (mob instanceof Creeper && difficulty >= 0.55) {
            mask |= PRESSURE_UNREACHABLE.bit | CREEPER_STALK.bit;
        }
        if (difficulty >= 0.75) {
            mask |= PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof EnderMan && difficulty >= 0.55) {
            mask |= ENDERMAN_DISRUPT.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof AbstractPiglin && difficulty >= 0.35) {
            mask |= PIGLIN_SOCIAL.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof Blaze && difficulty >= 0.45) {
            mask |= BLAZE_HOVER.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof Witch && difficulty >= 0.45) {
            mask |= WITCH_SUPPORT.bit | PRESSURE_UNREACHABLE.bit;
        }
        if ((mob instanceof Slime || mob instanceof MagmaCube) && difficulty >= 0.50) {
            mask |= SLIME_SURGE.bit;
        }
        if ((mob instanceof Hoglin || mob instanceof Zoglin) && difficulty >= 0.45) {
            mask |= HOGLIN_STAMPEDE.bit;
            if (difficulty >= 0.65) mask |= LEAP_UNREACHABLE.bit | MOB_STACK_CLIMB.bit;
        }
        if (IllagerInvasionCompat.isIllagerLike(mob) && difficulty >= 0.45) {
            mask |= ILLAGER_COMMAND.bit | PRESSURE_UNREACHABLE.bit;
            if (difficulty >= 0.70) mask |= LEAP_UNREACHABLE.bit | MOB_STACK_CLIMB.bit;
        }
        if (mob instanceof Phantom && difficulty >= 0.45) {
            mask |= PHANTOM_HARASS.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof Guardian && difficulty >= 0.45) {
            mask |= GUARDIAN_SURGE.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof Shulker && difficulty >= 0.50) {
            mask |= SHULKER_LOCKDOWN.bit;
        }
        if (mob instanceof Ghast && difficulty >= 0.45) {
            mask |= GHAST_REPOSITION.bit;
        }
        if (mob instanceof Ravager && difficulty >= 0.50) {
            mask |= RAVAGER_BREAKER.bit | PRESSURE_UNREACHABLE.bit;
        }
        if (mob instanceof Warden && difficulty >= 0.35) {
            mask |= WARDEN_PRESSURE.bit | PRESSURE_UNREACHABLE.bit;
        }
        return mask;
    }
}
