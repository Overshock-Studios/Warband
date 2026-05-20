package com.warband.spawn;

import com.warband.WarbandMod;
import com.warband.config.WarbandConfig;
import com.warband.difficulty.DifficultyManager;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * Spawn director — stamps difficulty onto naturally-spawned hostile mobs and
 * applies difficulty-scaled stat buffs.
 *
 * <p>The hook is {@code Mob#finalizeSpawn}, intercepted by
 * {@code com.warband.mixin.MobFinalizeSpawnMixin}, which calls
 * {@link #onMobFinalizeSpawn}.
 *
 * <p>Scope note: the "managed mob" set is {@link Enemy} (hostile mobs) — an
 * honest middle ground between a brittle explicit type list and "everything".
 *
 * <p>TODO (Phase 2b): wave/lull pacing — an L4D-style AI Director. The
 * {@code maxSmartMobsPerPlayer} cap is deferred to Phase 3, where tactical-AI
 * mobs (squads) actually exist; stamping and stat buffs are unbounded and cheap.
 */
public final class SpawnDirector {

    /** Spawn reasons we treat as "the world threw this at the player". */
    private static final EnumSet<EntitySpawnReason> WORLD_SPAWNS = EnumSet.of(
            EntitySpawnReason.NATURAL,
            EntitySpawnReason.CHUNK_GENERATION,
            EntitySpawnReason.STRUCTURE,
            EntitySpawnReason.PATROL,
            EntitySpawnReason.REINFORCEMENT,
            EntitySpawnReason.JOCKEY,
            EntitySpawnReason.EVENT);

    // Buff magnitudes at difficulty 1.0. Multiplicative on base value, except
    // knockback resistance which is a flat 0..1 add.
    private static final double HEALTH_BONUS = 1.20;
    private static final double DAMAGE_BONUS = 0.60;
    private static final double SPEED_BONUS = 0.15;
    private static final double KNOCKBACK_RESIST = 0.30;

    private static final Identifier HEALTH_MOD = modifierId("difficulty_health");
    private static final Identifier DAMAGE_MOD = modifierId("difficulty_damage");
    private static final Identifier SPEED_MOD = modifierId("difficulty_speed");
    private static final Identifier KNOCKBACK_MOD = modifierId("difficulty_knockback");

    private SpawnDirector() {
    }

    /**
     * Called from the {@code finalizeSpawn} mixin for every {@link Mob} the game
     * finalizes. Stamps managed hostile mobs with their local difficulty and,
     * if enabled, applies stat buffs.
     */
    public static void onMobFinalizeSpawn(Mob mob, ServerLevelAccessor accessor, EntitySpawnReason reason) {
        if (!(mob instanceof Enemy)) return;
        if (!WORLD_SPAWNS.contains(reason)) return;
        if (MobData.isStamped(mob)) return;

        ServerLevel level = accessor.getLevel();
        double difficulty = DifficultyManager.getDifficulty(level, mob.blockPosition());
        if (difficulty <= 0.0) return;

        stamp(mob, difficulty);
    }

    /**
     * Stamp a mob with an explicit difficulty and apply stat buffs. Used by the
     * natural-spawn path and by {@code /warband debug spawn}.
     */
    public static void stamp(Mob mob, double difficulty) {
        MobData.set(mob, new MobData((float) difficulty, Role.NONE, MobData.NO_SQUAD));
        if (WarbandConfig.statBuffsEnabled) {
            applyStatBuffs(mob, difficulty);
        }
    }

    private static void applyStatBuffs(Mob mob, double difficulty) {
        addMultiplied(mob, Attributes.MAX_HEALTH, HEALTH_MOD, difficulty * HEALTH_BONUS);
        addMultiplied(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MOD, difficulty * DAMAGE_BONUS);
        addMultiplied(mob, Attributes.MOVEMENT_SPEED, SPEED_MOD, difficulty * SPEED_BONUS);
        addFlat(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD, difficulty * KNOCKBACK_RESIST);
        // Health was modified above — refill so the mob spawns at its new maximum.
        mob.setHealth(mob.getMaxHealth());
    }

    private static void addMultiplied(Mob mob, Holder<Attribute> attribute, Identifier id, double amount) {
        addModifier(mob, attribute, id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void addFlat(Mob mob, Holder<Attribute> attribute, Identifier id, double amount) {
        addModifier(mob, attribute, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void addModifier(Mob mob, Holder<Attribute> attribute, Identifier id,
                                    double amount, AttributeModifier.Operation operation) {
        if (amount <= 0.0) return;
        AttributeInstance instance = mob.getAttribute(attribute);
        // Not every mob has every attribute (e.g. creepers have no attack damage).
        if (instance == null || instance.hasModifier(id)) return;
        instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Identifier modifierId(String path) {
        return Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, path);
    }
}
