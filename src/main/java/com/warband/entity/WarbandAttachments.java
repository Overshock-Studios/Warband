package com.warband.entity;

import com.mojang.serialization.Codec;
import com.warband.WarbandMod;
import com.warband.illager.FactionReputation;
import com.warband.illager.IllagerFactionData;
import com.warband.illager.IllagerGrudge;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Warband's Fabric data attachments.
 *
 * <p>This is the foundation every other system reads from: difficulty stamping
 * (Phase 2), squad assignment (Phase 3), and the difficulty-lens HUD (Phase 4)
 * all go through {@link #MOB_DATA}.
 */
public final class WarbandAttachments {

    /**
     * Per-mob {@link MobData}, stamped at spawn. Persistent — survives save/load.
     * Not copied on death: mobs do not respawn.
     */
    public static final AttachmentType<MobData> MOB_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "mob_data"),
            builder -> builder.persistent(MobData.CODEC)
    );

    /** Per-illager faction/doctrine identity. Persistent, separate from tactical squad state. */
    public static final AttachmentType<IllagerFactionData> ILLAGER_FACTION = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "illager_faction"),
            builder -> builder.persistent(IllagerFactionData.CODEC)
    );

    /** Per-mob marker: revenge/bounty attackers should not create fresh grudges. */
    public static final AttachmentType<Boolean> ILLAGER_GRUDGE_SPAWNED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "illager_grudge_spawned"),
            builder -> builder.persistent(Codec.BOOL)
    );

    /** Per-mob marker: this illager is a mansion's Warmarshal. Killing it breaks the faction. */
    public static final AttachmentType<Boolean> WARMARSHAL = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "warmarshal"),
            builder -> builder.persistent(Codec.BOOL)
    );

    /**
     * On a player: their capability score ({@code 0.0..1.0}) — the input to
     * SCORE-mode difficulty. Ratchets up instantly, decays down slowly; see
     * {@code com.warband.difficulty.PlayerScore}. Persistent and copied across
     * death so difficulty stays consistent session to session.
     */
    public static final AttachmentType<Float> PLAYER_SCORE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "player_score"),
            builder -> builder.persistent(Codec.FLOAT).copyOnDeath()
    );

    /**
     * On a player: the game-time tick until which post-death difficulty relief
     * applies. Absent when no relief is active. Not persistent — relief lapsing
     * on logout is acceptable.
     */
    public static final AttachmentType<Long> DEATH_RELIEF = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "death_relief")
    );

    /** On a player: named illager survivors that may return later with a patrol. */
    public static final AttachmentType<java.util.List<IllagerGrudge>> ILLAGER_GRUDGES = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "illager_grudges"),
            builder -> builder.persistent(IllagerGrudge.CODEC.listOf()).copyOnDeath()
    );

    /** On a player: faction heat used to trigger elite bounty hunters. */
    public static final AttachmentType<java.util.List<FactionReputation>> ILLAGER_REPUTATION = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "illager_reputation"),
            builder -> builder.persistent(FactionReputation.CODEC.listOf()).copyOnDeath()
    );

    private WarbandAttachments() {
    }

    /** Touch this class so its attachments register. Called from {@code onInitialize}. */
    public static void init() {
        // Intentionally empty — referencing the class triggers static init above.
    }
}
