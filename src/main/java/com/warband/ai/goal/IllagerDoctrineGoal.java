package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.IllagerLoadGuard;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.illager.FactionDoctrine;
import com.warband.illager.IllagerFactionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Military-style spacing and role movement for illager squads. */
public final class IllagerDoctrineGoal extends SquadGoal {

    private static final int COOLDOWN_TICKS = 45;

    private BlockPos doctrinePos;

    public IllagerDoctrineGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        if (!WarbandConfig.illagerDoctrineEnabled) return false;
        if (IllagerLoadGuard.tooDenseForHeavyDoctrine(mob)) return false;
        LivingEntity target = visibleTarget();
        if (target == null || squad.members().size() < 2 || !cooldownReady()) return false;

        Role role = MobData.get(mob).role();
        FactionDoctrine doctrine = IllagerFactionSystem.doctrineOrDefault(mob);
        double distance = mob.distanceTo(target);
        if (doctrine == FactionDoctrine.AMBUSH && role != Role.BRUISER) {
            doctrinePos = offsetAround(target, 11.0);
            return true;
        }
        if (doctrine == FactionDoctrine.SIEGE && role == Role.BRUISER) {
            doctrinePos = assault(target, distance + 2.0);
            return doctrinePos != null;
        }
        if (doctrine == FactionDoctrine.HUNT && distance > 10.0) {
            doctrinePos = offsetAround(target, 7.0);
            return true;
        }
        // BURN markers/support push closer to ignite — fire wants to be near you.
        if (doctrine == FactionDoctrine.BURN && (role == Role.MARKSMAN || role == Role.SUPPORT)
                && distance > 7.0) {
            doctrinePos = offsetAround(target, 6.0);
            return true;
        }
        // COMMAND holds a forward firing line — markers/leaders stand closer than
        // the default hold-line distance, presenting an organized front.
        if (doctrine == FactionDoctrine.COMMAND && (role == Role.MARKSMAN || role == Role.LEADER)
                && distance > 8.0 && distance < 20.0) {
            doctrinePos = offsetAround(target, 8.0);
            return true;
        }
        doctrinePos = switch (role) {
            case LEADER, SUPPORT, MARKSMAN -> holdLine(target, distance, role);
            case SKIRMISHER -> offsetAround(target, 8.0);
            case BRUISER -> assault(target, distance);
            default -> null;
        };
        return doctrinePos != null;
    }

    @Override
    public void start() {
        resetCooldown(COOLDOWN_TICKS);
        if (doctrinePos != null) {
            moveTo(doctrinePos);
        }
    }

    private BlockPos holdLine(LivingEntity target, double distance, Role role) {
        if (distance < 7.0) {
            BlockPos away = awayFrom(target.position(), role == Role.SUPPORT ? 10.0 : 7.0);
            return away;
        }
        if (distance > 18.0) {
            return offsetAround(target, 12.0);
        }
        if (role == Role.MARKSMAN || role == Role.LEADER) {
            return offsetAround(target, 10.0);
        }
        return null;
    }

    private BlockPos assault(LivingEntity target, double distance) {
        if (distance > 5.0) {
            return offsetAround(target, 3.0);
        }
        return null;
    }

    private BlockPos offsetAround(LivingEntity target, double radius) {
        int seed = Math.abs(mob.getUUID().hashCode());
        double angle = ((seed % 360) / 180.0) * Math.PI + mob.tickCount * 0.01;
        Vec3 pos = target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return BlockPos.containing(pos.x, pos.y, pos.z);
    }
}
