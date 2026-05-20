package com.warband.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

/** Soft integration for Illager Invasion without a compile-time dependency. */
public final class IllagerInvasionCompat {

    private static final String MOD_ID = "illagerinvasion";

    private IllagerInvasionCompat() {
    }

    public static boolean isIllagerLike(Mob mob) {
        return mob instanceof AbstractIllager || isIllagerInvasionMob(mob);
    }

    public static boolean isIllagerInvasionMob(Mob mob) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return MOD_ID.equals(id.getNamespace()) && switch (id.getPath()) {
            case "alchemist", "archivist", "basher", "firecaller", "inquisitor",
                    "invoker", "marauder", "necromancer", "provoker", "sorcerer" -> true;
            default -> false;
        };
    }

    public static boolean isSupport(Mob mob) {
        return switch (path(mob)) {
            case "alchemist", "archivist", "firecaller", "invoker", "necromancer", "provoker", "sorcerer" -> true;
            default -> false;
        };
    }

    public static boolean isSkirmisher(Mob mob) {
        return "marauder".equals(path(mob));
    }

    public static boolean isBruiser(Mob mob) {
        String path = path(mob);
        return "basher".equals(path) || "inquisitor".equals(path);
    }

    public static String roleTitle(Mob mob) {
        return switch (path(mob)) {
            case "alchemist" -> "Alchemist";
            case "archivist" -> "Archivist";
            case "basher" -> "Bulwark";
            case "firecaller" -> "Firecaller";
            case "inquisitor" -> "Inquisitor";
            case "invoker" -> "Invoker";
            case "marauder" -> "Marauder";
            case "necromancer" -> "Necromancer";
            case "provoker" -> "Provoker";
            case "sorcerer" -> "Sorcerer";
            default -> "";
        };
    }

    private static String path(Mob mob) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
    }
}
