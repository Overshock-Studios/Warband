package com.warband.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Shared audio/visual feedback for Warband tactical actions. */
public final class TacticalEffects {

    private TacticalEffects() {
    }

    public static void web(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 0.8f, 1.15f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                18, 0.35, 0.35, 0.35, 0.02);
    }

    public static void webTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z,
                    1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                to.x, to.y, to.z, 3, 0.12, 0.12, 0.12, 0.0);
    }

    public static void frost(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 0.6f, 1.35f);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5,
                10, 0.4, 0.08, 0.4, 0.01);
    }

    public static void smoke(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.COBWEB_BREAK, SoundSource.HOSTILE, 0.7f, 0.7f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.8, pos.z,
                28, 0.45, 0.45, 0.45, 0.03);
    }

    public static void signal(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1.0, pos.z,
                8, 0.3, 0.4, 0.3, 0.02);
    }

    public static void signal(ServerLevel level, Mob mob) {
        mob.playAmbientSound();
        signal(level, mob.position());
    }

    public static void horn(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.75f, 1.0f);
        signal(level, pos);
    }

    public static void search(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.ASH, pos.x, pos.y + 0.6, pos.z,
                6, 0.25, 0.2, 0.25, 0.01);
    }

    public static void waterCommit(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.2, pos.z,
                12, 0.35, 0.1, 0.35, 0.08);
    }
}
