package com.warband.entity;

import com.warband.compat.IllagerInvasionCompat;
import com.warband.config.WarbandConfig;
import com.warband.illager.IllagerFaction;
import com.warband.illager.IllagerFactionSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;

/** Assigns stable rank-style names to Warband illagers. */
public final class IllagerIdentity {

    private static final String[] NAMES = {
            "Arvek", "Borran", "Cald", "Dren", "Eskar", "Fenn", "Garrik", "Hask",
            "Ivek", "Jorren", "Karn", "Lorr", "Mavik", "Nesk", "Orren", "Pask",
            "Quell", "Rusk", "Sarn", "Tovik", "Urren", "Vask", "Werrik", "Yorn"
    };

    private IllagerIdentity() {
    }

    public static void assignIfNeeded(Mob mob, Role role, double difficulty) {
        if (!IllagerInvasionCompat.isIllagerLike(mob) || mob.hasCustomName()) return;

        String rank = rank(role, difficulty);
        String title = IllagerInvasionCompat.roleTitle(mob);
        if (!title.isEmpty() && role != Role.LEADER) {
            rank = title;
        }
        String name = NAMES[Math.floorMod(mob.getUUID().hashCode(), NAMES.length)];
        if (!WarbandConfig.illagerFactionsEnabled) {
            mob.setCustomName(Component.literal(rank + " " + name));
            // Hover-only: a visible custom name renders through walls, which reads as
        // a wallhack. Names show when the player looks at the mob.
        mob.setCustomNameVisible(false);
            return;
        }
        IllagerFaction faction = IllagerFactionSystem.factionOrDefault(mob);
        mob.setCustomName(Component.literal(rank + " " + name + " of the " + faction.displayName()));
        // Hover-only: a visible custom name renders through walls, which reads as
        // a wallhack. Names show when the player looks at the mob.
        mob.setCustomNameVisible(false);
    }

    /**
     * Re-name a mob as its stronghold's single Warmarshal. Called once per
     * mansion by {@code StrongholdGarrison}; overrides the rank from
     * {@link #assignIfNeeded}.
     */
    public static void promoteToWarmarshal(Mob mob) {
        if (!IllagerInvasionCompat.isIllagerLike(mob)) return;
        String name = NAMES[Math.floorMod(mob.getUUID().hashCode(), NAMES.length)];
        if (!WarbandConfig.illagerFactionsEnabled) {
            mob.setCustomName(Component.literal("Warmarshal " + name));
        } else {
            IllagerFaction faction = IllagerFactionSystem.factionOrDefault(mob);
            mob.setCustomName(Component.literal("Warmarshal " + name + " of the " + faction.displayName()));
        }
        mob.setCustomNameVisible(false);
    }

    private static String rank(Role role, double difficulty) {
        if (role == Role.LEADER) {
            // "Warmarshal" is not auto-assigned, it is a single per-stronghold
            // title granted by StrongholdGarrison via promoteToWarmarshal.
            if (difficulty >= 0.65) return "Captain";
            return "Sergeant";
        }
        if (role == Role.MARKSMAN) return difficulty >= 0.70 ? "Deadeye" : "Crossbowman";
        if (role == Role.SKIRMISHER) return "Raider";
        if (role == Role.SUPPORT) return "Standard";
        if (difficulty >= 0.70) return "Enforcer";
        return "Pillager";
    }
}
