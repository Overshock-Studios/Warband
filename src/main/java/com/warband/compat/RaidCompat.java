package com.warband.compat;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.raid.Raider;

/** Small wrapper around vanilla raid state so goals stay readable. */
public final class RaidCompat {

    private RaidCompat() {
    }

    public static boolean isActiveRaider(Mob mob) {
        return mob instanceof Raider raider && raider.getCurrentRaid() != null;
    }

    public static boolean isPatrolCaptain(Mob mob) {
        return mob instanceof Raider && mob instanceof PatrollingMonster patrollingMonster
                && patrollingMonster.isPatrolLeader();
    }
}
