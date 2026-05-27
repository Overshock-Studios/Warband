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

    /** Cue for revenge / war / crusade / bounty arrivals — see {@link ArrivalCue}. */
    public static void arrivalCue(ServerLevel level, Vec3 pos, ArrivalCue cue) {
        switch (cue) {
            case REVENGE -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0f, 0.85f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        pos.x, pos.y + 1.5, pos.z, 60, 2.5, 1.5, 2.5, 0.04);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        pos.x, pos.y + 1.8, pos.z, 20, 1.2, 0.8, 1.2, 0.0);
            }
            case BOUNTY -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GOAT_HORN_SOUND_VARIANTS.getFirst().value(), SoundSource.HOSTILE, 1.0f, 0.7f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 0.8f, 0.55f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        pos.x, pos.y + 1.3, pos.z, 80, 2.0, 1.5, 2.0, 0.03);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x, pos.y + 1.0, pos.z, 25, 1.0, 0.6, 1.0, 0.02);
            }
            case WAR_PATROL -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0f, 1.0f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 0.6f, 1.0f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        pos.x, pos.y + 1.5, pos.z, 40, 2.0, 1.2, 2.0, 0.03);
            }
            case CRUSADE -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.4f, 0.6f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.9f, 1.4f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.7f, 1.1f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        pos.x, pos.y + 2.0, pos.z, 120, 4.0, 2.0, 4.0, 0.05);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        pos.x, pos.y + 1.5, pos.z, 40, 3.0, 1.5, 3.0, 0.04);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        pos.x, pos.y + 1.5, pos.z, 30, 2.5, 1.5, 2.5, 0.06);
            }
        }
    }

    public enum ArrivalCue { REVENGE, BOUNTY, WAR_PATROL, CRUSADE }

    public static void search(ServerLevel level, Vec3 pos) {
    }

    public static void waterCommit(ServerLevel level, Vec3 pos) {
    }
}
