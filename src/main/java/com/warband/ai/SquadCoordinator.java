package com.warband.ai;

import com.warband.ai.goal.BreakLosGoal;
import com.warband.ai.goal.BlazeHoverGoal;
import com.warband.ai.goal.CallBackupGoal;
import com.warband.ai.goal.CreeperStalkGoal;
import com.warband.ai.goal.EndermanDisruptGoal;
import com.warband.ai.goal.ExtendedMobTacticGoal;
import com.warband.ai.goal.FlankGoal;
import com.warband.ai.goal.FrostWalkerGoal;
import com.warband.ai.goal.HoglinStampedeGoal;
import com.warband.ai.goal.IllagerCommandGoal;
import com.warband.ai.goal.IllagerDoctrineGoal;
import com.warband.ai.goal.IllagerRaidAssaultGoal;
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
import com.warband.compat.RaidCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.entity.Tactic;
import com.warband.mixin.MobGoalSelectorAccessor;
import com.warband.spawn.SpawnDirector;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    private static final Map<Integer, Squad> SQUADS = new HashMap<>();
    private static final double JOIN_RADIUS = 18.0;
    private static final double BACKUP_RADIUS = 32.0;
    private static final double SMART_SCAN_RADIUS = 96.0;
    /** Below this difficulty a mob fights to the death, only smarter mobs retreat. */
    private static final double RETREAT_MIN_DIFFICULTY = 0.35;
    /** Tactical-AI threshold, below this a mob stays vanilla. */
    private static final double SMART_MIN_DIFFICULTY = 0.20;
    /** Difficulty needed to crown a leader. */
    private static final double LEADER_MIN_DIFFICULTY = 0.40;
    private static int nextSquadId = 1;
    private static boolean spawningSquadmate;
    private static int squadTickCounter;

    private SquadCoordinator() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WarbandConfig.squadsEnabled) return;
            if (++squadTickCounter < 2) return;
            squadTickCounter = 0;

            Iterator<Squad> iterator = SQUADS.values().iterator();
            while (iterator.hasNext()) {
                Squad squad = iterator.next();
                squad.tick();
                if (squad.isEmpty()) {
                    iterator.remove();
                }
            }
        });

        // Re-attach goals to mobs loaded from disk. finalizeSpawn → ENTITY_LOAD
        // for fresh spawns, so the marker is already set and we skip.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!WarbandConfig.squadsEnabled) return;
            if (!(entity instanceof Mob mob)) return;
            if (!MobData.isStamped(mob)) return;
            if (Boolean.TRUE.equals(mob.getAttached(WarbandAttachments.WARBAND_GOALS_BOUND))) return;

            MobData data = MobData.get(mob);
            int squadId = data.squadId();
            Squad squad;
            if (squadId != MobData.NO_SQUAD) {
                Squad existing = SQUADS.get(squadId);
                squad = existing != null ? existing : new Squad(squadId, level);
                if (existing == null) SQUADS.put(squadId, squad);
                if (squadId >= nextSquadId) nextSquadId = squadId + 1;
                squad.add(mob);
            } else {
                squad = new Squad(MobData.NO_SQUAD, level);
            }
            addGoals(mob, squad, data.role());
        });
    }

    private static boolean hasWarbandGoals(Mob mob) {
        for (WrappedGoal wrapped : ((MobGoalSelectorAccessor) mob).warband$goalSelector().getAvailableGoals()) {
            if (wrapped.getGoal() instanceof WarbandGoal) return true;
        }
        return false;
    }

    /**
     * Adds this mob to a nearby active squad, or starts a new one if difficulty
     * warrants it and the local smart-mob cap allows it.
     */
    public static boolean assignNaturalSpawn(Mob mob, double difficulty) {
        return assignNaturalSpawn(mob, difficulty, true);
    }

    public static boolean assignNaturalSpawn(Mob mob, double difficulty, boolean spawnFormation) {
        if (!WarbandConfig.squadsEnabled || difficulty < SMART_MIN_DIFFICULTY || !underSmartCap((ServerLevel) mob.level(), mob)) {
            return false;
        }
        if (!isSmartEligible(mob)) {
            return false;
        }
        if (mob.getRandom().nextFloat() > squadChance(difficulty)) {
            return false;
        }

        ServerLevel level = (ServerLevel) mob.level();
        if (!formsNaturalSquads(mob)) {
            if (Tactic.chooseFor(mob, difficulty, Role.NONE) == 0) return false;
            addSoloTactics(level, mob, difficulty);
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
        if (newSquad && spawnFormation) {
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

    public static Squad createSquad(ServerLevel level, List<Mob> mobs, double difficulty) {
        Squad squad = new Squad(nextSquadId++, level);
        SQUADS.put(squad.id(), squad);

        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob == null || !mob.isAlive()) continue;
            Role role = i == 0 && difficulty >= 0.45 ? Role.LEADER : chooseRole(mob, i, difficulty);
            addMob(squad, mob, role, difficulty);
        }
        return squad;
    }

    /**
     * Make a mob the commander of a squad: guarantees squad membership, the
     * {@code ILLAGER_COMMAND} tactic and its goal kit, and a kite goal so the
     * commander fights from behind the line. This is what makes a Warmarshal the
     * <i>smartest</i> illager in its garrison, not merely the strongest.
     */
    public static void makeCommander(Mob mob, double difficulty) {
        if (!WarbandConfig.squadsEnabled || !(mob.level() instanceof ServerLevel level)) return;

        Squad squad = SQUADS.get(MobData.get(mob).squadId());
        if (squad == null || squad.isEmpty()) {
            squad = nearestJoinableSquad(level, mob.blockPosition(), mob);
        }
        if (squad == null) {
            squad = new Squad(nextSquadId++, level);
            SQUADS.put(squad.id(), squad);
        }

        int tactics = MobData.get(mob).tactics() | Tactic.ILLAGER_COMMAND.bit();
        MobData.set(mob, new MobData((float) difficulty, Role.LEADER, squad.id(), tactics));
        addGoals(mob, squad, Role.LEADER);
        // A commander directs from behind the line, kite, never facetank.
        ((MobGoalSelectorAccessor) mob).warband$goalSelector().addGoal(2, new KiteGoal(mob, squad));
        squad.add(mob);
    }

    public static boolean callBackup(Squad squad, BlockPos near) {
        int cap = effectiveMaxSquadSize(squad.level(), near);
        if (squad.members().size() >= cap) return false;

        AABB box = AABB.ofSize(near.getCenter(), BACKUP_RADIUS * 2.0, BACKUP_RADIUS, BACKUP_RADIUS * 2.0);
        List<Mob> candidates = squad.level().getEntitiesOfClass(Mob.class, box, mob -> {
            MobData data = MobData.get(mob);
            return data.squadId() != squad.id()
                    && canRecruitBackup(squad, mob)
                    && (!data.inSquad() || !isActiveSquad(data.squadId()));
        });

        // Only one backup mob per call, drip-feed, never a flood.
        if (candidates.isEmpty()) return false;
        double difficulty = squad.members().isEmpty() ? 0.5 : MobData.get(squad.members().getFirst()).difficulty();
        Mob recruit = candidates.getFirst();
        Role role = chooseRole(recruit, squad.members().size(), difficulty);
        addMob(squad, recruit, role, Math.max(difficulty, MobData.get(recruit).difficulty()));
        return true;
    }

    public static int activeSquads() {
        return SQUADS.size();
    }

    /** Lookup for perception hooks (e.g. arrow-miss alerts). */
    public static Squad getSquad(int id) {
        return SQUADS.get(id);
    }

    private static final double SQUAD_REGION_RADIUS = 64.0;
    private static final int MAX_EXTRA_PLAYERS = 8;

    /**
     * Squad-size cap, raised above {@code maxSquadSize} for players sharing the
     * region. Multiplayer scales encounter <i>volume</i> here, the per-mob
     * difficulty scalar still caps at 1.0.
     */
    private static int effectiveMaxSquadSize(ServerLevel level, BlockPos near) {
        int base = WarbandConfig.maxSquadSize;
        if (WarbandConfig.squadPlayerBonus <= 0) return base;
        AABB box = AABB.ofSize(near.getCenter(),
                SQUAD_REGION_RADIUS * 2.0, SQUAD_REGION_RADIUS * 2.0, SQUAD_REGION_RADIUS * 2.0);
        int players = level.getEntitiesOfClass(Player.class, box,
                player -> player.isAlive() && !player.isSpectator()).size();
        int extra = Math.min(MAX_EXTRA_PLAYERS, Math.max(0, players - 1));
        return base + WarbandConfig.squadPlayerBonus * extra;
    }

    private static void addMob(Squad squad, Mob mob, Role role, double difficulty) {
        SpawnDirector.stamp(mob, difficulty, role, squad.id());
        addGoals(mob, squad, role);
        squad.add(mob);
    }

    private static void addSoloTactics(ServerLevel level, Mob mob, double difficulty) {
        SpawnDirector.stamp(mob, difficulty);
        addGoals(mob, new Squad(MobData.NO_SQUAD, level), Role.NONE);
    }

    private static void addGoals(Mob mob, Squad squad, Role role) {
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) mob;
        accessor.warband$goalSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        accessor.warband$targetSelector().removeAllGoals(goal -> goal instanceof WarbandGoal);
        if (role != Role.NONE) {
            accessor.warband$targetSelector().addGoal(0, new SquadTargetGoal(mob, squad));
        }

        boolean simple = isSimpleFamily(mob);
        if (role != Role.NONE) {
            if (!simple && MobData.get(mob).difficulty() >= RETREAT_MIN_DIFFICULTY) {
                accessor.warband$goalSelector().addGoal(2, new RetreatWhenLowGoal(mob, squad));
            }
            accessor.warband$goalSelector().addGoal(3, new RegroupGoal(mob, squad));
            if (canCallBackup(mob)) {
                accessor.warband$goalSelector().addGoal(4, new CallBackupGoal(mob, squad));
            }
            accessor.warband$goalSelector().addGoal(6, new InvestigateLastKnownGoal(mob, squad));

            // Simple-family mobs (basic zombies, spiders, slimes, hoglins) follow
            // the squad target and regroup, but skip the more nuanced kite/flank
            // behaviors, those belong to smarter mobs.
            if (!simple) {
                if (role == Role.SKIRMISHER || role == Role.MARKSMAN || role == Role.SUPPORT) {
                    accessor.warband$goalSelector().addGoal(2, new KiteGoal(mob, squad));
                    accessor.warband$goalSelector().addGoal(3, new BreakLosGoal(mob, squad));
                } else {
                    accessor.warband$goalSelector().addGoal(5, new FlankGoal(mob, squad));
                }
            }
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
            accessor.warband$goalSelector().addGoal(1, new IllagerRaidAssaultGoal(mob, squad));
            accessor.warband$goalSelector().addGoal(2, new IllagerDoctrineGoal(mob, squad));
            accessor.warband$goalSelector().addGoal(3, new IllagerCommandGoal(mob, squad));
        }
        if (data.hasTactic(Tactic.PHANTOM_HARASS)) {
            accessor.warband$goalSelector().addGoal(3, new PhantomHarassGoal(mob, squad));
        }
        if (WarbandConfig.extendedMobTacticsEnabled
                && (data.hasTactic(Tactic.GUARDIAN_SURGE)
                || data.hasTactic(Tactic.SHULKER_LOCKDOWN)
                || data.hasTactic(Tactic.GHAST_REPOSITION)
                || data.hasTactic(Tactic.CAVE_SPIDER_AMBUSH)
                || data.hasTactic(Tactic.RAVAGER_BREAKER)
                || data.hasTactic(Tactic.WARDEN_PRESSURE))) {
            accessor.warband$goalSelector().addGoal(3, new ExtendedMobTacticGoal(mob, squad));
        }
        mob.setAttached(WarbandAttachments.WARBAND_GOALS_BOUND, true);
    }

    /**
     * The "simple" family: mobs whose AI we keep deliberately blunt, they follow
     * the squad target, regroup, and (if applicable) call backup, but skip kite,
     * flank, breakLOS, and retreat. Smarter mobs (skeletons, drowned, piglins,
     * illagers, witches, blazes, endermen) get the full kit.
     */
    private static boolean isSimpleFamily(Mob mob) {
        if (mob instanceof Drowned) return false;
        return mob instanceof Zombie
                || mob instanceof Spider
                || mob instanceof Slime
                || mob instanceof MagmaCube
                || mob instanceof Hoglin
                || mob instanceof Zoglin;
    }

    private static void spawnNaturalSquadmates(Squad squad, Mob anchor, double difficulty) {
        int cap = effectiveMaxSquadSize(squad.level(), anchor.blockPosition());
        if (difficulty < 0.45 || squad.members().size() >= cap) return;

        int baseSize = 2 + (int) Math.floor(difficulty * 3.0);
        if (isZombieFamily(anchor)) {
            baseSize += 1;
        }
        int desiredSize = Math.min(cap, baseSize);
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
        int cap = effectiveMaxSquadSize(level, pos);
        Squad best = null;
        double bestDist = JOIN_RADIUS * JOIN_RADIUS;
        for (Squad squad : SQUADS.values()) {
            if (squad.level() != level || squad.members().size() >= cap) continue;
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
        if (RaidCompat.isPatrolCaptain(mob)) return Role.LEADER;
        if (index == 0 && difficulty >= LEADER_MIN_DIFFICULTY) return Role.LEADER;
        // Consolidated roles: SUPPORT only when a compat layer flags it (e.g. witches
        // in illager invasions); MARKSMAN for anything ranged; otherwise BRUISER.
        // SKIRMISHER is no longer assigned procedurally, its kit collapses into
        // MARKSMAN, which shares the same kite/breakLOS goal block.
        if (IllagerInvasionCompat.isSupport(mob)) return Role.SUPPORT;
        if (mob instanceof RangedAttackMob) return Role.MARKSMAN;
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
                || mob instanceof Phantom
                || mob instanceof Guardian
                || mob instanceof Shulker
                || mob instanceof Ghast
                || mob instanceof CaveSpider
                || mob instanceof Ravager
                || mob instanceof Warden;
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
        if (!canCallBackup(mob) || difficulty < 0.35) return false;
        return mob.getRandom().nextFloat() < 0.45f;
    }

    private static double squadChance(double difficulty) {
        // Ramp faster: full-strength chance hits at diff ~0.70 (was 0.90).
        double t = Math.max(0.0, Math.min(1.0, (difficulty - SMART_MIN_DIFFICULTY) / 0.50));
        return WarbandConfig.naturalSquadChanceMin
                + (WarbandConfig.naturalSquadChanceMax - WarbandConfig.naturalSquadChanceMin) * t;
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
        List<Mob> smart = new ArrayList<>(level.getEntitiesOfClass(Mob.class, box, SquadCoordinator::hasWarbandAi));
        return smart.size() < WarbandConfig.maxSmartMobsPerPlayer;
    }

    private static boolean hasWarbandAi(Mob mob) {
        MobData data = MobData.get(mob);
        return data.inSquad() || data.tactics() != 0;
    }
}
