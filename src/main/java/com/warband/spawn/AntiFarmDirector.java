package com.warband.spawn;

import com.warband.config.WarbandConfig;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Detects obvious mob-farm conditions and turns them into low-value, escape-focused mobs. */
public final class AntiFarmDirector {

    private static int tickCounter;

    private AntiFarmDirector() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WarbandConfig.antiFarmEnabled) return;
            if (++tickCounter < WarbandConfig.antiFarmScanSeconds * 20) return;
            tickCounter = 0;

            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    AABB scan = AABB.ofSize(player.position(), 160.0, 80.0, 160.0);
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, scan,
                            candidate -> candidate instanceof Enemy && candidate.isAlive())) {
                        inspect(level, mob);
                    }
                }
            }
        });
    }

    public static boolean isFarmSuppressed(Mob mob) {
        return farmTier(mob) >= 2 || Boolean.TRUE.equals(mob.getAttached(WarbandAttachments.FARM_SUPPRESSED));
    }

    public static int farmTier(Mob mob) {
        Integer tier = mob.getAttached(WarbandAttachments.FARM_TIER);
        return tier != null ? tier : 0;
    }

    private static void inspect(ServerLevel level, Mob mob) {
        if (mob.getTarget() != null) return;
        AABB crowdBox = AABB.ofSize(mob.position(), 8.0, 4.0, 8.0);
        List<Mob> crowd = level.getEntitiesOfClass(Mob.class, crowdBox, other ->
                other.isAlive() && other.getType() == mob.getType());
        int tier = tierFor(level, mob, crowd.size());
        if (tier <= farmTier(mob)) return;

        mob.setAttached(WarbandAttachments.FARM_TIER, tier);
        if (tier >= 2) {
            mob.setAttached(WarbandAttachments.FARM_SUPPRESSED, true);
        }
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 20, tier >= 3 ? 2 : 1, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 20, tier >= 3 ? 2 : 1, false, true));
        if (tier >= 3) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 20, 0, false, true));
        }

        Player player = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 48.0, false);
        if (player != null) {
            mob.setTarget(player);
            mob.getNavigation().moveTo(player, tier >= 3 ? 1.45 : 1.25);
        } else {
            Vec3 shove = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.35, mob.getRandom().nextDouble() - 0.5).normalize().scale(tier >= 3 ? 0.65 : 0.35);
            mob.setDeltaMovement(mob.getDeltaMovement().add(shove));
        }
    }

    private static int tierFor(ServerLevel level, Mob mob, int crowdSize) {
        boolean hardTrap = isHardTrap(level, mob.blockPosition());
        if (crowdSize >= WarbandConfig.antiFarmTier3Crowd || (hardTrap && crowdSize >= WarbandConfig.antiFarmTier2Crowd)) {
            return 3;
        }
        if (crowdSize >= WarbandConfig.antiFarmTier2Crowd || hardTrap) {
            return 2;
        }
        if (crowdSize >= Math.min(WarbandConfig.antiFarmCrowdThreshold, WarbandConfig.antiFarmTier1Crowd)) {
            return 1;
        }
        return 0;
    }

    private static boolean isHardTrap(ServerLevel level, BlockPos pos) {
        int blocked = 0;
        for (BlockPos side : List.of(pos.north(), pos.south(), pos.east(), pos.west())) {
            if (!level.getBlockState(side).isAir() && !level.getBlockState(side).is(Blocks.WATER)) {
                blocked++;
            }
        }
        return blocked >= 3 && !level.getBlockState(pos.above()).isAir();
    }
}
