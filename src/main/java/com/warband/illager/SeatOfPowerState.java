package com.warband.illager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warband.WarbandMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent per-dimension state for faction seats: crowned and broken mansion-tier strongholds. */
public final class SeatOfPowerState extends SavedData {

    private static final Codec<SeatOfPowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("crownedSeats", List.of()).forGetter(SeatOfPowerState::crownedSeats),
            Codec.STRING.listOf().optionalFieldOf("brokenSeats", List.of()).forGetter(SeatOfPowerState::brokenSeats)
    ).apply(instance, SeatOfPowerState::new));

    private static final SavedDataType<SeatOfPowerState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, "seat_of_power_state"),
            SeatOfPowerState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES);

    private final Set<String> crownedSeats;
    private final Set<String> brokenSeats;

    public SeatOfPowerState() {
        this(Set.of(), Set.of());
    }

    private SeatOfPowerState(List<String> crownedSeats, List<String> brokenSeats) {
        this(new HashSet<>(crownedSeats), new HashSet<>(brokenSeats));
    }

    private SeatOfPowerState(Set<String> crownedSeats, Set<String> brokenSeats) {
        this.crownedSeats = crownedSeats;
        this.brokenSeats = brokenSeats;
    }

    public static SeatOfPowerState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static String key(ServerLevel level, int strongholdSeed) {
        return level.dimension().identifier() + ":" + strongholdSeed;
    }

    public boolean isCrowned(String key) {
        return crownedSeats.contains(key);
    }

    public boolean markCrowned(String key) {
        boolean changed = crownedSeats.add(key);
        if (changed) setDirty();
        return changed;
    }

    public boolean isBroken(String key) {
        return brokenSeats.contains(key);
    }

    public boolean markBroken(String key) {
        boolean changed = brokenSeats.add(key);
        if (changed) setDirty();
        return changed;
    }

    private List<String> crownedSeats() {
        return List.copyOf(crownedSeats);
    }

    private List<String> brokenSeats() {
        return List.copyOf(brokenSeats);
    }
}
