package com.warband.illager;

import com.warband.ai.SquadCoordinator;
import com.warband.compat.IllagerInvasionCompat;
import com.warband.compat.StructureCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.spawn.SpawnDirector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.HashSet;
import java.util.Set;

/**
 * Makes mansions and pillager outposts feel like faction strongholds: their
 * illagers spawn as an elevated garrison rather than scattered mobs.
 *
 * <p>A mansion's evokers and vindicators are placed straight from the structure
 * template as saved entities, they never pass through {@code Mob#finalizeSpawn},
 * so {@code SpawnDirector}'s mixin misses them. This catches them on entity load
 * instead. Naturally-spawned stronghold illagers (outpost pillagers) are handled
 * by {@code SpawnDirector} via {@link #floorFor}.
 */
public final class StrongholdGarrison {

    /** Mansion seeds that have already crowned their one Warmarshal. */
    private static final Set<Integer> WARMARSHAL_SEATS = new HashSet<>();

    private StrongholdGarrison() {
    }

    /** Register the entity-load garrison hook. Called from {@code onInitialize}. */
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!WarbandConfig.illagerStrongholdsEnabled) return;
            if (!(entity instanceof Mob mob) || MobData.isStamped(mob)) return;
            if (!IllagerInvasionCompat.isIllagerLike(mob)) return;

            BlockPos pos = mob.blockPosition();
            boolean seat = StructureCompat.inFactionSeat(level, pos);
            double floor = seat ? WarbandConfig.mansionGarrisonFloor
                    : StructureCompat.inFactionCamp(level, pos) ? WarbandConfig.outpostGarrisonFloor : 0.0;
            if (floor <= 0.0) return;

            // Organize existing garrison residents into squads, spawnFormation
            // is false so a mansion is not multiplied, only coordinated.
            if (!SquadCoordinator.assignNaturalSpawn(mob, floor, false)) {
                SpawnDirector.stamp(mob, floor);
            }

            // Exactly one Warmarshal per mansion, the first garrison illager to
            // muster claims the title; every other leader stays a Captain.
            if (seat) {
                Integer seed = StructureCompat.strongholdSeed(level, pos);
                if (seed != null && WARMARSHAL_SEATS.add(seed)) {
                    SpawnDirector.crownWarmarshal(mob);
                }
            }
        });
    }

    /**
     * Difficulty floor for the stronghold containing {@code pos}, the mansion
     * or outpost garrison level, or {@code 0.0} if it is not in a stronghold.
     */
    public static double floorFor(ServerLevel level, BlockPos pos) {
        if (!WarbandConfig.illagerStrongholdsEnabled) return 0.0;
        if (StructureCompat.inFactionSeat(level, pos)) return WarbandConfig.mansionGarrisonFloor;
        if (StructureCompat.inFactionCamp(level, pos)) return WarbandConfig.outpostGarrisonFloor;
        return 0.0;
    }
}
