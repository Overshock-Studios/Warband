package com.warband.entity;

import com.warband.WarbandMod;
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

    /**
     * On a player: the game-time tick until which post-death difficulty relief
     * applies. Absent when no relief is active. Not persistent — relief lapsing
     * on logout is acceptable.
     */
    public static final AttachmentType<Long> DEATH_RELIEF = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "death_relief")
    );

    private WarbandAttachments() {
    }

    /** Touch this class so its attachments register. Called from {@code onInitialize}. */
    public static void init() {
        // Intentionally empty — referencing the class triggers static init above.
    }
}
