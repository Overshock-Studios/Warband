package com.warband.compat;

import com.warband.WarbandMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

/**
 * Detects illager strongholds by <b>structure tag</b>, never by hardcoded
 * structure id. Vanilla structures are listed in the tag files shipped with the
 * mod; structure mods opt their own mansions/outposts in by adding to the same
 * tags. A modded pillager structure that opts into neither still works fine —
 * it just uses the generic origin-anchoring instead of stronghold treatment.
 */
public final class StructureCompat {

    /** Mansion-tier faction capitals — {@code data/warband/tags/worldgen/structure/faction_seats.json}. */
    public static final TagKey<Structure> FACTION_SEATS = TagKey.create(
            Registries.STRUCTURE, Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "faction_seats"));

    /** Outpost-tier faction forward camps. */
    public static final TagKey<Structure> FACTION_CAMPS = TagKey.create(
            Registries.STRUCTURE, Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "faction_camps"));

    private StructureCompat() {
    }

    public static boolean inFactionSeat(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, FACTION_SEATS).isValid();
    }

    public static boolean inFactionCamp(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, FACTION_CAMPS).isValid();
    }

    /**
     * A stable seed for the stronghold containing {@code pos}, so a whole
     * structure resolves to one faction. {@code null} if not in a stronghold.
     */
    public static @Nullable Integer strongholdSeed(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, FACTION_SEATS);
        if (!start.isValid()) {
            start = level.structureManager().getStructureWithPieceAt(pos, FACTION_CAMPS);
        }
        if (!start.isValid()) {
            return null;
        }
        BoundingBox box = start.getBoundingBox();
        return box.minX() * 31 + box.minZ();
    }
}
