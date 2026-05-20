package com.warband.ally;

import com.warband.WarbandMod;
import com.warband.ai.goal.GolemDefendGoal;
import com.warband.ai.goal.GolemSpinGoal;
import com.warband.ai.goal.WarbandGoal;
import com.warband.difficulty.DifficultyManager;
import com.warband.mixin.MobGoalSelectorAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/** Gives village/player defenders enough help to survive Warband-level threats. */
public final class GolemDirector {

    private static final int TICK_INTERVAL = 40;
    private static final double PLAYER_SCAN_RADIUS = 96.0;

    private static final Identifier HEALTH_MOD = id("golem_health");
    private static final Identifier DAMAGE_MOD = id("golem_damage");
    private static final Identifier SPEED_MOD = id("golem_speed");
    private static final Identifier KNOCKBACK_MOD = id("golem_knockback");
    private static final Identifier ARMOR_MOD = id("golem_armor");
    private static final Identifier FOLLOW_RANGE_MOD = id("golem_follow_range");

    private static int tickCounter;

    private GolemDirector() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < TICK_INTERVAL) return;
            tickCounter = 0;

            for (ServerLevel level : server.getAllLevels()) {
                tickLevel(level);
            }
        });
    }

    private static void tickLevel(ServerLevel level) {
        Set<AbstractGolem> golems = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            AABB box = AABB.ofSize(player.position(), PLAYER_SCAN_RADIUS * 2.0, PLAYER_SCAN_RADIUS, PLAYER_SCAN_RADIUS * 2.0);
            golems.addAll(level.getEntitiesOfClass(AbstractGolem.class, box, golem -> golem instanceof Mob));
        }

        for (AbstractGolem golem : golems) {
            double difficulty = DifficultyManager.getDifficulty(level, golem.blockPosition());
            if (difficulty < 0.20) continue;
            enhance(golem, difficulty);
            injectGoal((Mob) golem);
        }
    }

    private static void enhance(AbstractGolem golem, double difficulty) {
        if (golem instanceof IronGolem) {
            addMultiplied(golem, Attributes.MAX_HEALTH, HEALTH_MOD, difficulty * 0.80);
            addMultiplied(golem, Attributes.ATTACK_DAMAGE, DAMAGE_MOD, difficulty * 0.40);
            addMultiplied(golem, Attributes.MOVEMENT_SPEED, SPEED_MOD, difficulty * 0.10);
            addFlat(golem, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD, difficulty * 0.45);
        } else if (golem instanceof SnowGolem) {
            addMultiplied(golem, Attributes.MAX_HEALTH, HEALTH_MOD, difficulty * 2.50);
            addMultiplied(golem, Attributes.MOVEMENT_SPEED, SPEED_MOD, difficulty * 0.20);
            addFlat(golem, Attributes.ARMOR, ARMOR_MOD, difficulty * 6.0);
            addMultiplied(golem, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MOD, difficulty * 0.75);
        }
        golem.setHealth(Math.max(golem.getHealth(), golem.getMaxHealth() * 0.65f));
    }

    private static void injectGoal(Mob golem) {
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) golem;
        accessor.warband$goalSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        accessor.warband$targetSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        accessor.warband$targetSelector().addGoal(0, new GolemDefendGoal(golem));
        if (golem instanceof IronGolem ironGolem) {
            accessor.warband$goalSelector().addGoal(1, new GolemSpinGoal(ironGolem));
        }
    }

    private static void addMultiplied(AbstractGolem golem, Holder<Attribute> attribute, Identifier id, double amount) {
        addModifier(golem, attribute, id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void addFlat(AbstractGolem golem, Holder<Attribute> attribute, Identifier id, double amount) {
        addModifier(golem, attribute, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void addModifier(AbstractGolem golem, Holder<Attribute> attribute, Identifier id,
                                    double amount, AttributeModifier.Operation operation) {
        if (amount <= 0.0) return;
        AttributeInstance instance = golem.getAttribute(attribute);
        if (instance == null || instance.hasModifier(id)) return;
        instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WarbandMod.MOD_ID, path);
    }
}
