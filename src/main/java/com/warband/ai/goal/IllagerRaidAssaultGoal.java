package com.warband.ai.goal;

import com.warband.ai.Squad;
import com.warband.ai.TacticalEffects;
import com.warband.compat.RaidCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.illager.FactionDoctrine;
import com.warband.illager.IllagerFactionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/** Raid doctrine: prioritize village defenders and keep pressure on the settlement. */
public final class IllagerRaidAssaultGoal extends SquadGoal {

    public IllagerRaidAssaultGoal(Mob mob, Squad squad) {
        super(mob, squad, 1.2);
    }

    @Override
    public boolean canUse() {
        if (!WarbandConfig.illagerRaidDoctrineEnabled) return false;
        if (!RaidCompat.isActiveRaider(mob) || !decisionReady(35)) return false;

        LivingEntity priority = priorityTarget();
        if (priority == null) return false;

        mob.setTarget(priority);
        Role role = MobData.get(mob).role();
        FactionDoctrine doctrine = IllagerFactionSystem.doctrineOrDefault(mob);
        if (role == Role.LEADER) {
            priority.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
            TacticalEffects.signal((ServerLevel) mob.level(), mob);
        }
        if (role == Role.BRUISER || role == Role.SKIRMISHER || doctrine == FactionDoctrine.PURGE) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, doctrine == FactionDoctrine.HUNT ? 1 : 0, false, true));
        }
        if (doctrine == FactionDoctrine.COMMAND && squad.members().size() >= 3) {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, true));
        }
        if (doctrine == FactionDoctrine.BURN && priority instanceof Villager) {
            priority.setRemainingFireTicks(Math.max(priority.getRemainingFireTicks(), 60));
        }
        if (role == Role.MARKSMAN || role == Role.SUPPORT || role == Role.LEADER) {
            return moveTo(priority.blockPosition().offset(
                    mob.getRandom().nextInt(9) - 4,
                    0,
                    mob.getRandom().nextInt(9) - 4));
        }
        return moveTo(priority.blockPosition());
    }

    private LivingEntity priorityTarget() {
        LivingEntity current = visibleTarget();
        if (current instanceof Villager || current instanceof IronGolem) {
            return current;
        }

        AABB box = AABB.ofSize(mob.position(), 48.0, 24.0, 48.0);
        List<LivingEntity> candidates = ((ServerLevel) mob.level()).getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity instanceof Villager || entity instanceof IronGolem));
        return candidates.stream()
                .min(Comparator.comparingDouble(entity -> {
                    double distance = mob.distanceToSqr(entity);
                    return entity instanceof Villager ? distance : distance + 24.0;
                }))
                .orElse(current);
    }
}
