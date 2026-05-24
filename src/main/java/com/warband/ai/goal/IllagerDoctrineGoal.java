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

    public IllagerDoctrineGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.05);
    }

    @Override
    public boolean canUse() {
        if (!WarbandConfig.illagerDoctrineEnabled) return false;
        if (IllagerLoadGuard.tooDenseForHeavyDoctrine(mob)) return false;
        LivingEntity target = visibleTarget();
        if (target == null || squad.members().size() < 2 || !decisionReady(45)) return false;

        Role role = MobData.get(mob).role();
        FactionDoctrine doctrine = IllagerFactionSystem.doctrineOrDefault(mob);
        double distance = mob.distanceTo(target);
        if (doctrine == FactionDoctrine.AMBUSH && role != Role.BRUISER) {
            return flank(target, 11.0);
        }
        if (doctrine == FactionDoctrine.SIEGE && role == Role.BRUISER) {
            return assault(target, distance + 2.0);
        }
        if (doctrine == FactionDoctrine.HUNT && distance > 10.0) {
            return moveTo(offsetAround(target, 7.0));
        }
        // BURN markers/support push closer to ignite — fire wants to be near you.
        if (doctrine == FactionDoctrine.BURN && (role == Role.MARKSMAN || role == Role.SUPPORT)
                && distance > 7.0) {
            return moveTo(offsetAround(target, 6.0));
        }
        // COMMAND holds a forward firing line — markers/leaders stand closer than
        // the default hold-line distance, presenting an organized front.
        if (doctrine == FactionDoctrine.COMMAND && (role == Role.MARKSMAN || role == Role.LEADER)
                && distance > 8.0 && distance < 20.0) {
            return moveTo(offsetAround(target, 8.0));
        }
        return switch (role) {
            case LEADER, SUPPORT, MARKSMAN -> holdLine(target, distance, role);
            case SKIRMISHER -> flank(target, 8.0);
            case BRUISER -> assault(target, distance);
            default -> false;
        };
    }

    private boolean holdLine(LivingEntity target, double distance, Role role) {
        if (distance < 7.0) {
            BlockPos away = awayFrom(target.position(), role == Role.SUPPORT ? 10.0 : 7.0);
            return away != null && moveTo(away);
        }
        if (distance > 18.0) {
            return moveTo(offsetAround(target, 12.0));
        }
        if (role == Role.MARKSMAN || role == Role.LEADER) {
            return moveTo(offsetAround(target, 10.0));
        }
        return false;
    }

    private boolean assault(LivingEntity target, double distance) {
        if (distance > 5.0) {
            return moveTo(offsetAround(target, 3.0));
        }
        return false;
    }

    private boolean flank(LivingEntity target, double radius) {
        return moveTo(offsetAround(target, radius));
    }

    private BlockPos offsetAround(LivingEntity target, double radius) {
        int seed = Math.abs(mob.getUUID().hashCode());
        double angle = ((seed % 360) / 180.0) * Math.PI + mob.tickCount * 0.01;
        Vec3 pos = target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return BlockPos.containing(pos.x, pos.y, pos.z);
    }
}
