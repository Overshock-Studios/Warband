package com.warband.ai;

import com.warband.compat.IllagerInvasionCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Cheap density guard for outposts and other illager-heavy structures. */
public final class IllagerLoadGuard {

    private static final int DENSE_ILLAGER_LIMIT = 10;

    private IllagerLoadGuard() {
    }

    public static boolean tooDenseForHeavyDoctrine(Mob mob) {
        if (!IllagerInvasionCompat.isIllagerLike(mob) || !(mob.level() instanceof ServerLevel level)) return false;
        AABB box = AABB.ofSize(mob.position(), 48.0, 24.0, 48.0);
        int count = level.getEntitiesOfClass(Mob.class, box, candidate ->
                candidate.isAlive() && IllagerInvasionCompat.isIllagerLike(candidate)).size();
        return count > DENSE_ILLAGER_LIMIT;
    }
}
