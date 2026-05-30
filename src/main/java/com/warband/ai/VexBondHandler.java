package com.warband.ai;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.phys.AABB;

/**
 * When an Evoker dies, any vexes it summoned die with it. Severs the
 * "summoned minions outlive their summoner" awkwardness and lets a player who
 * just killed the summoner actually see the encounter end.
 */
public final class VexBondHandler {

    private static final double SCAN_RADIUS = 48.0;

    private VexBondHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Evoker evoker)) return;
            if (!(evoker.level() instanceof ServerLevel level)) return;
            AABB box = AABB.ofSize(evoker.position(), SCAN_RADIUS * 2, SCAN_RADIUS, SCAN_RADIUS * 2);
            for (Vex vex : level.getEntitiesOfClass(Vex.class, box)) {
                if (vex.getOwner() == evoker) {
                    vex.kill(level);
                }
            }
        });
    }
}
