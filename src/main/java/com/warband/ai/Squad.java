package com.warband.ai;

import com.warband.entity.MobData;
import com.warband.entity.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight squad blackboard shared by tactical goals.
 *
 * <p>It stores only information the squad plausibly knows: visible targets,
 * last-known target position, morale, members, and a throttled backup cooldown.
 */
public final class Squad {

    private static final int LAST_KNOWN_TICKS = 20 * 12;
    private static final int BACKUP_COOLDOWN_TICKS = 20 * 30;
    /** How long a squad routs after its leader falls. */
    private static final int ROUT_TICKS = 20 * 8;
    /** Perception (line-of-sight) is refreshed on this tick cadence, not every tick. */
    private static final int PERCEPTION_INTERVAL = 5;

    private final int id;
    private final ServerLevel level;
    private final List<Mob> members = new ArrayList<>();
    private @Nullable LivingEntity target;
    private @Nullable BlockPos lastKnownPos;
    private int lastKnownTicks;
    private int backupCooldown;
    private int routTicks;
    private int perceptionCounter;
    private boolean hadLeader;
    private float morale = 1.0f;

    Squad(int id, ServerLevel level) {
        this.id = id;
        this.level = level;
    }

    public int id() {
        return id;
    }

    public ServerLevel level() {
        return level;
    }

    public List<Mob> members() {
        return members;
    }

    public @Nullable LivingEntity target() {
        return target;
    }

    public @Nullable BlockPos lastKnownPos() {
        return lastKnownPos;
    }

    public float morale() {
        return morale;
    }

    public boolean canCallBackup() {
        return backupCooldown <= 0;
    }

    /** True while the squad is routing, the leader recently fell. */
    public boolean isRouting() {
        return routTicks > 0;
    }

    public void markBackupCalled() {
        backupCooldown = BACKUP_COOLDOWN_TICKS;
    }

    /**
     * External perception cue (e.g. a missed arrow). Seeds the squad's
     * last-known position so its InvestigateLastKnownGoal moves them to look.
     */
    public void alertTo(BlockPos pos) {
        lastKnownPos = pos;
        lastKnownTicks = LAST_KNOWN_TICKS;
    }

    public void add(Mob mob) {
        if (!members.contains(mob)) {
            members.add(mob);
        }
    }

    public void tick() {
        members.removeIf(mob -> !mob.isAlive() || mob.isRemoved() || MobData.get(mob).squadId() != id);
        if (members.isEmpty()) return;

        if (backupCooldown > 0) {
            backupCooldown--;
        }
        if (routTicks > 0) {
            routTicks--;
        }
        updateMorale();
        if (++perceptionCounter >= PERCEPTION_INTERVAL) {
            perceptionCounter = 0;
            updatePerception();
        }
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public Vec3 center() {
        if (members.isEmpty()) {
            return lastKnownPos == null ? Vec3.ZERO : Vec3.atCenterOf(lastKnownPos);
        }

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (Mob mob : members) {
            x += mob.getX();
            y += mob.getY();
            z += mob.getZ();
        }
        double count = members.size();
        return new Vec3(x / count, y / count, z / count);
    }

    private void updateMorale() {
        boolean leaderAlive = false;
        for (Mob mob : members) {
            if (MobData.get(mob).role() == Role.LEADER) {
                leaderAlive = true;
                break;
            }
        }
        // The leader just fell, the squad routs for a window, then steadies and
        // re-engages (demoralized). A temporary break, not a permanent flee.
        if (hadLeader && !leaderAlive) {
            routTicks = ROUT_TICKS;
        }
        hadLeader = leaderAlive;
        morale = leaderAlive ? Math.min(1.0f, morale + 0.01f) : Math.max(0.2f, morale - 0.02f);
    }

    private void updatePerception() {
        List<LivingEntity> visible = new ArrayList<>();
        for (Mob mob : members) {
            LivingEntity candidate = mob.getTarget();
            if (candidate != null && candidate.isAlive() && mob.hasLineOfSight(candidate)) {
                visible.add(candidate);
                lastKnownPos = candidate.blockPosition();
                lastKnownTicks = LAST_KNOWN_TICKS;
            }
        }

        LivingEntity visibleTarget = visible.isEmpty()
                ? null
                : MultiplayerDirector.chooseSquadTarget(this, members.getFirst(), visible);
        target = visibleTarget;
        if (visibleTarget == null && lastKnownTicks > 0) {
            lastKnownTicks -= PERCEPTION_INTERVAL;
        }
        if (lastKnownTicks <= 0) {
            lastKnownPos = null;
        }
    }
}
