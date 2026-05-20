package com.warband.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;

/**
 * Warband's per-mob state, stamped at spawn and carried for the mob's lifetime.
 *
 * <p>Attached to the entity via the Fabric Data Attachment API (see
 * {@link WarbandAttachments}) — entities cannot carry data components, which are
 * item-stack-scoped. The attachment is persistent, so this survives save/load.
 *
 * @param difficulty the normalized {@code 0.0..1.0} difficulty at spawn
 * @param role       the mob's tactical role, or {@link Role#NONE} if unsquadded
 * @param squadId    the owning squad's id, or {@link #NO_SQUAD}
 * @param tactics    bitmask of assigned {@link Tactic}s
 */
public record MobData(float difficulty, Role role, int squadId, int tactics) {

    /** Sentinel {@link #squadId} for a mob that belongs to no squad. */
    public static final int NO_SQUAD = -1;

    /** Value returned for an unstamped mob — vanilla-calm, no squad. */
    public static final MobData DEFAULT = new MobData(0.0f, Role.NONE, NO_SQUAD, 0);

    public static final Codec<MobData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("difficulty").forGetter(MobData::difficulty),
            Role.CODEC.fieldOf("role").forGetter(MobData::role),
            Codec.INT.fieldOf("squadId").forGetter(MobData::squadId),
            Codec.INT.optionalFieldOf("tactics", 0).forGetter(MobData::tactics)
    ).apply(instance, MobData::new));

    public MobData(float difficulty, Role role, int squadId) {
        this(difficulty, role, squadId, 0);
    }

    /** This mob's data, or {@link #DEFAULT} if it was never stamped by Warband. */
    public static MobData get(Entity entity) {
        MobData data = entity.getAttached(WarbandAttachments.MOB_DATA);
        return data != null ? data : DEFAULT;
    }

    /** {@code true} if Warband stamped this mob (vs. a plain vanilla spawn). */
    public static boolean isStamped(Entity entity) {
        return entity.hasAttached(WarbandAttachments.MOB_DATA);
    }

    /** Stamp (or overwrite) this mob's Warband data. */
    public static void set(Entity entity, MobData data) {
        entity.setAttached(WarbandAttachments.MOB_DATA, data);
    }

    public boolean inSquad() {
        return squadId != NO_SQUAD;
    }

    public boolean hasTactic(Tactic tactic) {
        return Tactic.has(tactics, tactic);
    }
}
