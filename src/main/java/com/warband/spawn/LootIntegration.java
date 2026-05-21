package com.warband.spawn;

import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Optional, conservative bridge to Ascension Cores drops. */
public final class LootIntegration {

    private static final Identifier ASCENSION_CORE = Identifier.fromNamespaceAndPath("ascensioncores", "ascension_core");
    private static final Identifier CHAOS_CORE = Identifier.fromNamespaceAndPath("ascensioncores", "chaos_core");

    private LootIntegration() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(LootIntegration::afterDeath);
    }

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        if (!WarbandConfig.ascensionCoreLootEnabled || !FabricLoader.getInstance().isModLoaded("ascensioncores")) return;
        if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (!(source.getEntity() instanceof ServerPlayer)) return;
        if (!MobData.isStamped(mob) || AntiFarmDirector.isFarmSuppressed(mob)) return;

        MobData data = MobData.get(mob);
        double difficulty = data.difficulty();
        if (difficulty < 0.45) return;

        double roleBonus = data.inSquad() ? 0.004 : 0.0;
        double ascensionChance = WarbandConfig.ascensionCoreDropBaseChance
                + difficulty * difficulty * WarbandConfig.ascensionCoreDropDifficultyChance
                + roleBonus;
        if (mob.getRandom().nextDouble() < ascensionChance) {
            drop(level, mob, ASCENSION_CORE, 1);
        }

        double chaosChance = difficulty * difficulty * difficulty * WarbandConfig.chaosCoreDropDifficultyChance;
        if (data.role().isLeader() && mob.getRandom().nextDouble() < chaosChance) {
            drop(level, mob, CHAOS_CORE, 1);
        }
    }

    private static void drop(ServerLevel level, Mob mob, Identifier id, int count) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), new ItemStack(item.get(), count));
        level.addFreshEntity(entity);
    }
}
