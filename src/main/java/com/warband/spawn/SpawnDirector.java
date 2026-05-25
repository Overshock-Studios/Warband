package com.warband.spawn;

import com.warband.WarbandMod;
import com.warband.ai.SquadCoordinator;
import com.warband.compat.IllagerInvasionCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.IllagerIdentity;
import com.warband.difficulty.DifficultyManager;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.entity.WarbandAttachments;
import com.warband.entity.RoleVisuals;
import com.warband.entity.Tactic;
import com.warband.illager.IllagerFactionSystem;
import com.warband.illager.FactionBanner;
import com.warband.illager.IllagerLoadout;
import com.warband.illager.StrongholdGarrison;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * Spawn director, stamps difficulty onto naturally-spawned hostile mobs and
 * applies difficulty-scaled stat buffs.
 *
 * <p>The hook is {@code Mob#finalizeSpawn}, intercepted by
 * {@code com.warband.mixin.MobFinalizeSpawnMixin}, which calls
 * {@link #onMobFinalizeSpawn}.
 *
 * <p>Scope note: the "managed mob" set is {@link Enemy} (hostile mobs), an
 * honest middle ground between a brittle explicit type list and "everything".
 *
 * <p>TODO (Phase 2b): wave/lull pacing, an L4D-style AI Director. Tactical
 * squad assignment is Phase 3 and is delegated to {@link SquadCoordinator}.
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

    private static final Identifier HEALTH_MOD = modifierId("difficulty_health");
    private static final Identifier DAMAGE_MOD = modifierId("difficulty_damage");
    private static final Identifier SPEED_MOD = modifierId("difficulty_speed");
    private static final Identifier KNOCKBACK_MOD = modifierId("difficulty_knockback");
    private static final Identifier WARMARSHAL_HEALTH = modifierId("warmarshal_health");
    private static final Identifier WARMARSHAL_DAMAGE = modifierId("warmarshal_damage");

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
        if (SquadCoordinator.isSpawningSquadmate()) return;
        if (BossDirector.isSpawningWitherMinion()) return;
        if (MobData.isStamped(mob)) return;

        ServerLevel level = accessor.getLevel();
        // Bail out if we're running on a worldgen worker thread (e.g. C2ME structure
        // piece spawns). Any chunk-touching call here would deadlock the server
        // thread against the worker waiting on chunk completion. Vanilla structure
        // mobs spawned in that path are picked up later by tryStampLoaded when
        // their chunk loads on the main thread.
        if (level.getServer() != null && !level.getServer().isSameThread()) {
            return;
        }
        runStampPipeline(mob, level, reason);
    }

    /**
     * Called from the SquadCoordinator ENTITY_LOAD hook on the main thread for
     * Enemy mobs that were never stamped (typically structure-piece spawns from
     * off-thread worldgen). Treats them like a natural spawn.
     */
    public static void tryStampLoaded(Mob mob, ServerLevel level) {
        if (!(mob instanceof Enemy)) return;
        if (MobData.isStamped(mob)) return;
        if (SquadCoordinator.isSpawningSquadmate()) return;
        if (BossDirector.isSpawningWitherMinion()) return;
        if (level.getServer() != null && !level.getServer().isSameThread()) return;
        runStampPipeline(mob, level, EntitySpawnReason.NATURAL);
    }

    private static void runStampPipeline(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        double difficulty = DifficultyManager.getDifficulty(level, mob.blockPosition());
        if (IllagerInvasionCompat.isIllagerLike(mob)) {
            // Naturally-spawned stronghold illagers (e.g. outpost pillagers) are
            // raised to the garrison floor here; mansion residents are caught on
            // entity load by StrongholdGarrison instead.
            difficulty = Math.max(difficulty, StrongholdGarrison.floorFor(level, mob.blockPosition()));
        }
        if (difficulty <= 0.0) {
            stampIllagerIdentityOnly(mob, difficulty);
            return;
        }
        if (!EncounterDirector.allowsEnhancement(level, mob.blockPosition(), mob)) {
            stampIllagerIdentityOnly(mob, difficulty);
            return;
        }

        boolean spawnFormation = (!IllagerInvasionCompat.isIllagerLike(mob) && reason == EntitySpawnReason.NATURAL)
                || reason == EntitySpawnReason.PATROL
                || reason == EntitySpawnReason.EVENT;
        if (!SquadCoordinator.assignNaturalSpawn(mob, difficulty, spawnFormation)) {
            stampVanillaAi(mob, difficulty);
            SquadCoordinator.bindStampedSolo(mob, level);
        }
    }

    /**
     * Stamp a mob with an explicit difficulty and apply stat buffs. Used by the
     * natural-spawn path and by {@code /warband debug spawn}.
     */
    public static void stamp(Mob mob, double difficulty) {
        stamp(mob, difficulty, true);
    }

    /**
     * Stamp a mob that was enhanced statistically but was not selected for
     * tactical AI. Keeping the tactic mask clear prevents reload-only abilities.
     */
    public static void stampVanillaAi(Mob mob, double difficulty) {
        stamp(mob, difficulty, false);
    }

    private static void stamp(Mob mob, double difficulty, boolean assignTactics) {
        int tactics = assignTactics ? Tactic.chooseFor(mob, difficulty, Role.NONE) : 0;
        MobData.set(mob, new MobData((float) difficulty, Role.NONE, MobData.NO_SQUAD, tactics));
        IllagerFactionSystem.assignIfNeeded(mob);
        FactionBanner.equipIfNeeded(mob);
        IllagerIdentity.assignIfNeeded(mob, Role.NONE, difficulty);
        if (WarbandConfig.statBuffsEnabled) {
            applyStatBuffs(mob, difficulty);
        }
    }

    /** Stamp a mob with explicit squad metadata. */
    public static void stamp(Mob mob, double difficulty, Role role, int squadId) {
        int tactics = Tactic.chooseFor(mob, difficulty, role);
        MobData.set(mob, new MobData((float) difficulty, role, squadId, tactics));
        IllagerFactionSystem.assignIfNeeded(mob);
        FactionBanner.equipIfNeeded(mob);
        RoleVisuals.apply(mob, role, difficulty);
        IllagerLoadout.equip(mob, role, difficulty);
        IllagerIdentity.assignIfNeeded(mob, role, difficulty);
        if (WarbandConfig.statBuffsEnabled) {
            applyStatBuffs(mob, difficulty);
        }
    }

    /**
     * Crown a stamped illager as its mansion's Warmarshal, the faction's apex.
     * Deliberately outranks a bounty hunter: forced to maximum difficulty, given
     * a heavy boss health/damage layer and lasting Strength, and renamed.
     */
    public static void crownWarmarshal(Mob mob) {
        // The command-AI upgrade: guarantees command goals and squad leadership,
        // and sets the mob's MobData (difficulty 1.0, LEADER, ILLAGER_COMMAND).
        // A Warmarshal is the smartest illager in the garrison, not just the
        // strongest, without this it would be the bounty-hunter mistake again.
        SquadCoordinator.makeCommander(mob, 1.0);
        if (WarbandConfig.statBuffsEnabled) {
            addMultiplied(mob, Attributes.MAX_HEALTH, WARMARSHAL_HEALTH, WarbandConfig.warmarshalHealthBonus);
            addMultiplied(mob, Attributes.ATTACK_DAMAGE, WARMARSHAL_DAMAGE, WarbandConfig.warmarshalDamageBonus);
            mob.setHealth(mob.getMaxHealth());
        }
        // Strength, not Resistance: a Warmarshal is hard through health, damage,
        // its garrison and AI, never a damage sponge.
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.STRENGTH, 20 * 60 * 60, 1, false, true));
        mob.setAttached(WarbandAttachments.WARMARSHAL, true);
        IllagerIdentity.promoteToWarmarshal(mob);
    }

    private static void applyStatBuffs(Mob mob, double difficulty) {
        // Linear in difficulty: tactical AI switches on at ~0.25, so enhanced mobs
        // need real durability through the mid-game, not a back-loaded square curve.
        double statScale = difficulty;
        addMultiplied(mob, Attributes.MAX_HEALTH, HEALTH_MOD, statScale * WarbandConfig.statHealthBonusMax);
        addMultiplied(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MOD, statScale * WarbandConfig.statDamageBonusMax);
        addMultiplied(mob, Attributes.MOVEMENT_SPEED, SPEED_MOD, statScale * WarbandConfig.statSpeedBonusMax);
        addFlat(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD, statScale * WarbandConfig.statKnockbackResistanceMax);
        // Health was modified above, refill so the mob spawns at its new maximum.
        mob.setHealth(mob.getMaxHealth());
        maybeChargeCreeper(mob, difficulty);
    }

    /**
     * High-difficulty creepers have a chance to spawn naturally charged, the
     * difficulty band's spectacle reward. Implemented via a visual-only lightning
     * bolt + direct thunderHit so the world doesn't catch fire or take damage.
     */
    private static void maybeChargeCreeper(Mob mob, double difficulty) {
        if (!(mob instanceof Creeper creeper)) return;
        if (creeper.isPowered()) return;
        if (difficulty < 0.55) return;
        if (!(creeper.level() instanceof ServerLevel level)) return;
        // Ramps from 0% at diff 0.55 to ~30% at diff 1.0.
        double chance = (difficulty - 0.55) * 0.67;
        if (creeper.getRandom().nextDouble() >= chance) return;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt == null) return;
        bolt.snapTo(creeper.getX(), creeper.getY(), creeper.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        creeper.thunderHit(level, bolt);
    }

    private static void stampIllagerIdentityOnly(Mob mob, double difficulty) {
        if (!IllagerInvasionCompat.isIllagerLike(mob)) return;
        IllagerFactionSystem.assignIfNeeded(mob);
        FactionBanner.equipIfNeeded(mob);
        IllagerIdentity.assignIfNeeded(mob, Role.NONE, difficulty);
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
