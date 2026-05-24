package com.warband.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * A mob's tactical role within a squad.
 *
 * <p>This enum only <i>names</i> the roles, the behaviour behind each is built
 * in Phase 3 (squad AI). {@link #NONE} is the default for a stamped-but-unsquadded
 * mob.
 */
public enum Role implements StringRepresentable {

    /** Not part of a squad, a lone stamped mob. */
    NONE("none"),

    /** Closes distance, soaks hits. */
    BRUISER("bruiser"),

    /** Kites, breaks line of sight. */
    SKIRMISHER("skirmisher"),

    /** Ranged; seeks elevation. */
    MARKSMAN("marksman"),

    /** Heals / buffs squadmates. */
    SUPPORT("support"),

    /** Aura buff; squad morale collapses if it dies. */
    LEADER("leader");

    public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

    private final String serializedName;

    Role(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public boolean isLeader() {
        return this == LEADER;
    }
}
