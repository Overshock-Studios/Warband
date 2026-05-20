package com.warband.entity;

import com.warband.compat.IllagerInvasionCompat;
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
        mob.setCustomName(Component.literal(rank + " " + name));
        mob.setCustomNameVisible(difficulty >= 0.65 || role == Role.LEADER);
    }

    private static String rank(Role role, double difficulty) {
        if (role == Role.LEADER) {
            if (difficulty >= 0.85) return "Warmarshal";
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
