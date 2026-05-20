package com.warband.ai;

import com.warband.ai.goal.BreakLosGoal;
import com.warband.ai.goal.BlazeHoverGoal;
import com.warband.ai.goal.CallBackupGoal;
import com.warband.ai.goal.CreeperStalkGoal;
import com.warband.ai.goal.EndermanDisruptGoal;
import com.warband.ai.goal.FlankGoal;
import com.warband.ai.goal.FrostWalkerGoal;
import com.warband.ai.goal.HoglinStampedeGoal;
import com.warband.ai.goal.IllagerCommandGoal;
import com.warband.ai.goal.InvestigateLastKnownGoal;
import com.warband.ai.goal.KiteGoal;
import com.warband.ai.goal.PhantomHarassGoal;
import com.warband.ai.goal.PiglinSocialGoal;
import com.warband.ai.goal.PressureUnreachableGoal;
import com.warband.ai.goal.RegroupGoal;
import com.warband.ai.goal.RetreatWhenLowGoal;
import com.warband.ai.goal.SkeletonSmokeGoal;
import com.warband.ai.goal.SlimeSurgeGoal;
import com.warband.ai.goal.SquadTargetGoal;
import com.warband.ai.goal.SpiderWebGoal;
import com.warband.ai.goal.StickyPathGoal;
import com.warband.ai.goal.WaterCommitGoal;
import com.warband.ai.goal.WarbandGoal;
import com.warband.ai.goal.WitchSupportGoal;
import com.warband.ai.goal.ZombieHordeGoal;
import com.warband.compat.IllagerInvasionCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.entity.Tactic;
import com.warband.mixin.MobGoalSelectorAccessor;
import com.warband.spawn.SpawnDirector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry and throttled server tick driver for tactical squads.
 *
 * <p>Squads are runtime blackboards; mob attachments persist only the stable
 * squad id and role. On spawn/debug creation this coordinator attaches goals
 * and keeps the blackboard fresh.
 */
public final class SquadCoordinator {

    private static final Map<Integer, Squad> SQUADS = new LinkedHashMap<>();
    private static final double JOIN_RADIUS = 18.0;
    private static final double BACKUP_RADIUS = 32.0;
    private static final double SMART_SCAN_RADIUS = 96.0;
    private static int nextSquadId = 1;
    private static boolean spawningSquadmate;

    private SquadCoordinator() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WarbandConfig.squadsEnabled) return;

            Iterator<Squad> iterator = SQUADS.values().iterator();
            while (iterator.hasNext()) {
                Squad squad = iterator.next();
                squad.tick();
                if (squad.isEmpty()) {
                    iterator.remove();
                }
            }
        });
    }

    /**
     * Adds this mob to a nearby active squad, or starts a new one if difficulty
     * warrants it and the local smart-mob cap allows it.
     */
    public static boolean assignNaturalSpawn(Mob mob, double difficulty) {
        if (!WarbandConfig.squadsEnabled || difficulty < 0.25 || !underSmartCap((ServerLevel) mob.level(), mob)) {
            return false;
        }
        if (!isSmartEligible(mob)) {
            return false;
        }

        ServerLevel level = (ServerLevel) mob.level();
        if (!formsNaturalSquads(mob)) {
            if (Tactic.chooseFor(mob, difficulty, Role.NONE) == 0) return false;
            Squad solo = new Squad(nextSquadId++, level);
            SQUADS.put(solo.id(), solo);
            addMob(solo, mob, Role.NONE, difficulty);
            return true;
        }

        Squad squad = shouldJoinExisting(mob, difficulty)
                ? nearestJoinableSquad(level, mob.blockPosition(), mob)
                : null;
        boolean newSquad = false;
        if (squad == null) {
            squad = new Squad(nextSquadId++, level);
            SQUADS.put(squad.id(), squad);
            newSquad = true;
        }

        Role role = chooseRole(mob, squad.members().size(), difficulty);
        addMob(squad, mob, role, difficulty);
        if (newSquad) {
            spawnNaturalSquadmates(squad, mob, difficulty);
        }
        return true;
    }

    public static boolean isSpawningSquadmate() {
        return spawningSquadmate;
    }

    public static Squad createDebugSquad(ServerLevel level, BlockPos origin, double difficulty) {
        Squad squad = new Squad(nextSquadId++, level);
        SQUADS.put(squad.id(), squad);

        int size = Math.min(WarbandConfig.maxSquadSize, 3 + (int) Math.round(difficulty * 3.0));
        for (int i = 0; i < size; i++) {
            BlockPos pos = origin.offset((i % 3) - 1, 0, 2 + (i / 3));
            Zombie zombie = EntityType.ZOMBIE.spawn(level, pos, EntitySpawnReason.COMMAND);
            if (zombie == null) continue;

            Role role = switch (i) {
                case 0 -> Role.LEADER;
                case 1 -> Role.BRUISER;
                case 2 -> Role.SKIRMISHER;
                default -> Role.BRUISER;
            };
            SpawnDirector.stamp(zombie, difficulty, role, squad.id());
            addGoals(zombie, squad, role);
            squad.add(zombie);
        }
        return squad;
    }

    public static boolean callBackup(Squad squad, BlockPos near) {
        if (squad.members().size() >= WarbandConfig.maxSquadSize) return false;

        AABB box = AABB.ofSize(near.getCenter(), BACKUP_RADIUS * 2.0, BACKUP_RADIUS, BACKUP_RADIUS * 2.0);
        List<Mob> candidates = squad.level().getEntitiesOfClass(Mob.class, box, mob -> {
            MobData data = MobData.get(mob);
            return data.squadId() != squad.id()
                    && canRecruitBackup(squad, mob)
                    && (!data.inSquad() || !isActiveSquad(data.squadId()));
        });

        int joined = 0;
        int max = Math.min(1, WarbandConfig.maxSquadSize - squad.members().size());
        double difficulty = squad.members().isEmpty() ? 0.5 : MobData.get(squad.members().getFirst()).difficulty();
        for (Mob mob : candidates) {
            if (joined >= max) break;
            Role role = chooseRole(mob, squad.members().size(), difficulty);
            addMob(squad, mob, role, Math.max(difficulty, MobData.get(mob).difficulty()));
            joined++;
        }
        return joined > 0;
    }

    public static int activeSquads() {
        return SQUADS.size();
    }

    private static void addMob(Squad squad, Mob mob, Role role, double difficulty) {
        SpawnDirector.stamp(mob, difficulty, role, squad.id());
        addGoals(mob, squad, role);
        squad.add(mob);
    }

    private static void addGoals(Mob mob, Squad squad, Role role) {
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) mob;
        accessor.warband$goalSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        accessor.warband$targetSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        accessor.warband$targetSelector().addGoal(0, new SquadTargetGoal(mob, squad));
        accessor.warband$goalSelector().addGoal(2, new RetreatWhenLowGoal(mob, squad));
        accessor.warband$goalSelector().addGoal(3, new RegroupGoal(mob, squad));
        if (canCallBackup(mob)) {
            accessor.warband$goalSelector().addGoal(4, new CallBackupGoal(mob, squad));
        }
        accessor.warband$goalSelector().addGoal(6, new InvestigateLastKnownGoal(mob, squad));

        if (role == Role.SKIRMISHER || role == Role.MARKSMAN || role == Role.SUPPORT) {
            accessor.warband$goalSelector().addGoal(2, new KiteGoal(mob, squad));
            accessor.warband$goalSelector().addGoal(3, new BreakLosGoal(mob, squad));
        } else {
            accessor.warband$goalSelector().addGoal(5, new FlankGoal(mob, squad));
        }

        MobData data = MobData.get(mob);
        if (data.hasTactic(Tactic.SPIDER_WEB)) {
            accessor.warband$goalSelector().addGoal(3, new SpiderWebGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.STICKY_PATH)) {
            accessor.warband$goalSelector().addGoal(7, new StickyPathGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.FROST_WALKER)) {
            accessor.warband$goalSelector().addGoal(7, new FrostWalkerGoal(mob));
        }
        if (data.hasTactic(Tactic.WATER_COMMIT)) {
            accessor.warband$goalSelector().addGoal(4, new WaterCommitGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.PRESSURE_UNREACHABLE)) {
            accessor.warband$goalSelector().addGoal(5, new PressureUnreachableGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.SKELETON_SMOKE) && mob instanceof AbstractSkeleton) {
            accessor.warband$goalSelector().addGoal(2, new SkeletonSmokeGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.CREEPER_STALK)) {
            accessor.warband$goalSelector().addGoal(4, new CreeperStalkGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.ZOMBIE_HORDE)) {
            accessor.warband$goalSelector().addGoal(4, new ZombieHordeGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.ENDERMAN_DISRUPT) && mob instanceof EnderMan) {
            accessor.warband$goalSelector().addGoal(3, new EndermanDisruptGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.PIGLIN_SOCIAL)) {
            accessor.warband$goalSelector().addGoal(3, new PiglinSocialGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.BLAZE_HOVER)) {
            accessor.warband$goalSelector().addGoal(3, new BlazeHoverGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.WITCH_SUPPORT)) {
            accessor.warband$goalSelector().addGoal(3, new WitchSupportGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.SLIME_SURGE)) {
            accessor.warband$goalSelector().addGoal(4, new SlimeSurgeGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.HOGLIN_STAMPEDE)) {
            accessor.warband$goalSelector().addGoal(4, new HoglinStampedeGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.ILLAGER_COMMAND)) {
            accessor.warband$goalSelector().addGoal(3, new IllagerCommandGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.PHANTOM_HARASS)) {
            accessor.warband$goalSelector().addGoal(3, new PhantomHarassGoal(mob, squad));
        }
    }

    private static void spawnNaturalSquadmates(Squad squad, Mob anchor, double difficulty) {
        if (difficulty < 0.45 || squad.members().size() >= WarbandConfig.maxSquadSize) return;

        int baseSize = 2 + (int) Math.floor(difficulty * 4.0);
        if (isZombieFamily(anchor)) {
            baseSize += 1;
        }
        int desiredSize = Math.min(WarbandConfig.maxSquadSize, baseSize);
        int toSpawn = desiredSize - squad.members().size();
        for (int i = 0; i < toSpawn; i++) {
            if (!underSmartCap(squad.level(), anchor)) break;

            BlockPos pos = anchor.blockPosition().offset(
                    anchor.getRandom().nextInt(7) - 3,
                    0,
                    anchor.getRandom().nextInt(7) - 3);
            Mob spawned = spawnSameType(anchor, pos);
            if (spawned == null) continue;

            Role role = chooseRole(spawned, squad.members().size(), difficulty);
            addMob(squad, spawned, role, difficulty);
            TacticalEffects.signal(squad.level(), spawned.position());
        }
    }

    @SuppressWarnings("unchecked")
    private static Mob spawnSameType(Mob anchor, BlockPos pos) {
        EntityType<? extends Mob> type = (EntityType<? extends Mob>) anchor.getType();
        spawningSquadmate = true;
        try {
            return type.spawn((ServerLevel) anchor.level(), pos, EntitySpawnReason.REINFORCEMENT);
        } finally {
            spawningSquadmate = false;
        }
    }

    private static Squad nearestJoinableSquad(ServerLevel level, BlockPos pos, Mob mob) {
        Squad best = null;
        double bestDist = 10.0 * 10.0;
        for (Squad squad : SQUADS.values()) {
            if (squad.level() != level || squad.members().size() >= WarbandConfig.maxSquadSize) continue;
            if (squad.members().isEmpty() || !sameSquadFamily(squad.members().getFirst(), mob)) continue;
            double dist = squad.center().distanceToSqr(pos.getCenter());
            if (dist < bestDist) {
                best = squad;
                bestDist = dist;
            }
        }
        return best;
    }

    private static Role chooseRole(Mob mob, int index, double difficulty) {
        if (index == 0 && difficulty >= 0.45) return Role.LEADER;
        if (IllagerInvasionCompat.isSupport(mob)) return Role.SUPPORT;
        if (IllagerInvasionCompat.isSkirmisher(mob)) return Role.SKIRMISHER;
        if (mob instanceof RangedAttackMob) return Role.MARKSMAN;
        if (IllagerInvasionCompat.isBruiser(mob)) return Role.BRUISER;
        if (difficulty >= 0.55 && index % 3 == 2) return Role.SKIRMISHER;
        return Role.BRUISER;
    }

    private static boolean isActiveSquad(int squadId) {
        return SQUADS.containsKey(squadId);
    }

    private static boolean isSmartEligible(Mob mob) {
        return mob instanceof Zombie
                || mob instanceof Drowned
                || mob instanceof ZombifiedPiglin
                || mob instanceof AbstractSkeleton
                || mob instanceof Spider
                || mob instanceof Creeper
                || mob instanceof EnderMan
                || mob instanceof AbstractPiglin
                || mob instanceof Blaze
                || mob instanceof Witch
                || mob instanceof Slime
                || mob instanceof MagmaCube
                || mob instanceof Hoglin
                || mob instanceof Zoglin
                || IllagerInvasionCompat.isIllagerLike(mob)
                || mob instanceof Phantom;
    }

    private static boolean formsNaturalSquads(Mob mob) {
        return mob instanceof Zombie
                || mob instanceof Drowned
                || mob instanceof ZombifiedPiglin
                || mob instanceof AbstractSkeleton
                || mob instanceof Spider
                || mob instanceof AbstractPiglin
                || mob instanceof Hoglin
                || mob instanceof Zoglin
                || IllagerInvasionCompat.isIllagerLike(mob);
    }

    private static boolean shouldJoinExisting(Mob mob, double difficulty) {
        if (!canCallBackup(mob) || difficulty < 0.50) return false;
        return mob.getRandom().nextFloat() < 0.25f;
    }

    private static boolean canCallBackup(Mob mob) {
        return mob instanceof Zombie
                || mob instanceof Drowned
                || mob instanceof ZombifiedPiglin
                || mob instanceof Spider
                || mob instanceof AbstractPiglin
                || mob instanceof Hoglin
                || mob instanceof Zoglin
                || IllagerInvasionCompat.isIllagerLike(mob);
    }

    private static boolean canRecruitBackup(Squad squad, Mob candidate) {
        return !squad.members().isEmpty()
                && canCallBackup(candidate)
                && sameSquadFamily(squad.members().getFirst(), candidate);
    }

    private static boolean sameSquadFamily(Mob a, Mob b) {
        if (isZombieFamily(a) || isZombieFamily(b)) {
            return isZombieFamily(a) && isZombieFamily(b);
        }
        if (a instanceof AbstractSkeleton || b instanceof AbstractSkeleton) {
            return a instanceof AbstractSkeleton && b instanceof AbstractSkeleton;
        }
        if (a instanceof Spider || b instanceof Spider) {
            return a instanceof Spider && b instanceof Spider;
        }
        if (a instanceof AbstractPiglin || b instanceof AbstractPiglin) {
            return a instanceof AbstractPiglin && b instanceof AbstractPiglin;
        }
        if (a instanceof Hoglin || a instanceof Zoglin || b instanceof Hoglin || b instanceof Zoglin) {
            return (a instanceof Hoglin || a instanceof Zoglin) && (b instanceof Hoglin || b instanceof Zoglin);
        }
        if (IllagerInvasionCompat.isIllagerLike(a) || IllagerInvasionCompat.isIllagerLike(b)) {
            return IllagerInvasionCompat.isIllagerLike(a) && IllagerInvasionCompat.isIllagerLike(b);
        }
        return a.getType() == b.getType();
    }

    private static boolean isZombieFamily(Mob mob) {
        return mob instanceof Zombie || mob instanceof Drowned || mob instanceof ZombifiedPiglin;
    }

    private static boolean underSmartCap(ServerLevel level, Mob mob) {
        Player nearest = level.getNearestPlayer(
                mob.getX(), mob.getY(), mob.getZ(), SMART_SCAN_RADIUS, false);
        if (nearest == null) return true;

        AABB box = AABB.ofSize(nearest.position(), SMART_SCAN_RADIUS * 2.0, SMART_SCAN_RADIUS, SMART_SCAN_RADIUS * 2.0);
        List<Mob> smart = new ArrayList<>(level.getEntitiesOfClass(Mob.class, box, candidate -> MobData.get(candidate).inSquad()));
        return smart.size() < WarbandConfig.maxSmartMobsPerPlayer;
    }
}
