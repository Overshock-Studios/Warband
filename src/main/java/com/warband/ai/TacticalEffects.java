package com.warband.ai;

import com.warband.config.WarbandConfig;
import com.warband.entity.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Shared audio/visual feedback for Warband tactical actions. */
public final class TacticalEffects {

    private TacticalEffects() {
    }

    public static void web(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 0.8f, 1.15f);
    }

    public static void webTrail(ServerLevel level, Vec3 from, Vec3 to) {
    }

    public static void frost(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 0.6f, 1.35f);
    }

    public static void smoke(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.COBWEB_BREAK, SoundSource.HOSTILE, 0.7f, 0.7f);
    }

    public static void signal(ServerLevel level, Vec3 pos) {
    }

    public static void signal(ServerLevel level, Mob mob) {
        mob.playAmbientSound();
        signal(level, mob.position());
    }

    public static void roleCue(Mob mob, Role role) {
        if (!WarbandConfig.roleCuesEnabled || !(mob.level() instanceof ServerLevel level)) return;
        float pitch = switch (role) {
            case LEADER -> 0.70f;
            case BRUISER -> 0.82f;
            case MARKSMAN -> 1.10f;
            case SKIRMISHER -> 1.25f;
            case SUPPORT -> 1.00f;
            case NONE -> 1.00f;
        };
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 0.25f, pitch);
    }

    public static void horn(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.75f, 1.0f);
        signal(level, pos);
    }

    public static void search(ServerLevel level, Vec3 pos) {
    }

    public static void waterCommit(ServerLevel level, Vec3 pos) {
    }
}
