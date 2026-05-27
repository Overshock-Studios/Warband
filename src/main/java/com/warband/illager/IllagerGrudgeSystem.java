package com.warband.illager;

import com.warband.advancement.WarbandCriteria;
import com.warband.ai.SquadCoordinator;
import com.warband.ai.TacticalEffects;
import com.warband.ai.MultiplayerDirector;
import com.warband.compat.IllagerInvasionCompat;
import com.warband.compat.RaidCompat;
import com.warband.compat.StructureCompat;
import com.warband.config.WarbandConfig;
import com.warband.entity.MobData;
import com.warband.entity.Role;
import com.warband.entity.WarbandAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Local "raid ledger" loop for named illager survivors and revenge patrols. */
public final class IllagerGrudgeSystem {

    private static final int SURVIVE_CONFIRM_TICKS = 20 * 60;
    private static final int SURVIVOR_EXPIRY_TICKS = 20 * 60 * 10;
    private static final int REVENGE_DELAY_TICKS = 20 * 60 * 3;
    private static final int RETRY_DELAY_TICKS = 20 * 60 * 3;
    /** If the player is within this of a grudge's origin, revenge musters there. */
    private static final int ORIGIN_MUSTER_RANGE = 160;
    private static final int SCAN_INTERVAL_TICKS = 20 * 10;
    private static final int MAX_GRUDGES_PER_PLAYER = 8;
    private static final int MAX_REPUTATION_RECORDS = 5;
    private static final int BOUNTY_HEAT_THRESHOLD = 100;
    private static final int BOUNTY_RETRY_TICKS = 20 * 60 * 10;
    private static final int SPAWN_MIN_DISTANCE = 28;
    private static final int SPAWN_RANGE = 18;
    private static final double WITNESS_RADIUS = 28.0;
    private static final int RIVAL_INTERCEPT_CHANCE = 25;
    /** Faction heat added per illager killed inside that faction's mansion. */
    private static final int MANSION_HEAT_PER_KILL = 8;
    /** Small heat drip for any factioned illager killed in the open. Builds toward bounty heat over time. */
    private static final int FIELD_HEAT_PER_KILL = 2;
    /** Witness count above which the notability filter is relaxed — a crowd remembers regardless of rank. */
    private static final int CROWD_WITNESS_THRESHOLD = 3;
    /** Cooldown between unprompted WAR-state patrols (per faction, per player). */
    private static final int WAR_PATROL_COOLDOWN_TICKS = 20 * 60 * 12;
    /** Cooldown between full CRUSADE assaults (per faction, per player). */
    private static final int CRUSADE_COOLDOWN_TICKS = 20 * 60 * 30;
    /** Heat shed by a faction whenever it commits a crusade — wars cannot stay perpetually maxed. */
    private static final int CRUSADE_HEAT_RELIEF = 35;

    private static final Map<UUID, PendingSurvivor> PENDING = new HashMap<>();
    private static int tickCounter;

    private IllagerGrudgeSystem() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(IllagerGrudgeSystem::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(IllagerGrudgeSystem::afterDeath);
        ServerTickEvents.END_SERVER_TICK.register(IllagerGrudgeSystem::tick);
    }

    private static void afterDamage(LivingEntity entity, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Mob mob) || damageTaken <= 0.0f || entity.isDeadOrDying()) return;
        if (!WarbandConfig.illagerGrudgesEnabled) return;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (!IllagerInvasionCompat.isIllagerLike(mob) || !MobData.isStamped(mob) || RaidCompat.isActiveRaider(mob)) {
            return;
        }
        if (isGrudgeSpawned(mob)) return;
        // Only badly wounded illagers stir their comrades, and only the first such
        // hit scans, the victim lands in PENDING below, so a drawn-out fight is
        // not a per-swing entity scan.
        if (mob.getHealth() / mob.getMaxHealth() > 0.5f) return;
        if (PENDING.containsKey(mob.getUUID())) return;
        // A mansion is a faction seat, the consequence lands on the kill (heat),
        // handled in afterDeath; no field-style witness grudges here.
        if (inFactionSeat(mob)) return;

        for (ServerPlayer participant : participantsNear((ServerLevel) mob.level(), mob, player)) {
            recordWitnesses((ServerLevel) mob.level(), mob, participant, true, false);
        }
    }

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        if (WarbandConfig.illagerGrudgesEnabled
                && entity instanceof Mob mob
                && source.getEntity() instanceof ServerPlayer player
                && IllagerInvasionCompat.isIllagerLike(mob)
                && MobData.isStamped(mob)
                && !isGrudgeSpawned(mob)
                && !RaidCompat.isActiveRaider(mob)) {
            IllagerFaction faction = IllagerFactionSystem.factionOrDefault(mob);
            long now = mob.level().getGameTime();
            if (inFactionSeat(mob)) {
                // Assaulting a faction's seat raises its heat (→ a bounty hunter)
                // rather than mustering revenge patrols back into the ruin.
                for (ServerPlayer participant : participantsNear((ServerLevel) mob.level(), mob, player)) {
                    addReputation(participant, faction, MANSION_HEAT_PER_KILL, now + BOUNTY_RETRY_TICKS);
                }
            } else {
                for (ServerPlayer participant : participantsNear((ServerLevel) mob.level(), mob, player)) {
                    // Every factioned kill nudges heat — slow drip so /warband intel
                    // shows the faction is "watching" even before a notable grudge forms.
                    addReputation(participant, faction, FIELD_HEAT_PER_KILL, now + BOUNTY_RETRY_TICKS);
                    boolean firstKill = !Boolean.TRUE.equals(participant.getAttached(WarbandAttachments.FIRST_KILL_GRACE_USED));
                    recordWitnesses((ServerLevel) mob.level(), mob, participant, true, firstKill);
                    if (firstKill) {
                        participant.setAttached(WarbandAttachments.FIRST_KILL_GRACE_USED, true);
                        WarbandCriteria.fire(participant, WarbandCriteria.FACTION_NOTICED);
                    }
                }
            }
        }
        handleWarmarshalDeath(entity, source);
        PENDING.remove(entity.getUUID());
    }

    /** A slain Warmarshal breaks its faction, clears the killer's grudges and heat with it. */
    private static void handleWarmarshalDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Mob mob)) return;
        if (!Boolean.TRUE.equals(mob.getAttached(WarbandAttachments.WARMARSHAL))) return;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        IllagerFaction faction = IllagerFactionSystem.factionOrDefault(mob);
        clearFaction(player, faction);
        WarbandCriteria.fire((ServerPlayer) source.getEntity(), WarbandCriteria.WARMARSHAL_SLAIN);
        player.sendSystemMessage(Component.literal(
                "The " + faction.displayName() + " falls into disarray, its Warmarshal is dead."));
    }

    /** Wipe a faction's grudges and reputation from a player, its war with them is over. */
    private static void clearFaction(ServerPlayer player, IllagerFaction faction) {
        List<IllagerGrudge> grudges = new ArrayList<>(grudges(player));
        if (grudges.removeIf(grudge -> grudge.faction() == faction)) {
            player.setAttached(WarbandAttachments.ILLAGER_GRUDGES, List.copyOf(grudges));
        }
        List<FactionReputation> reputations = new ArrayList<>(reputations(player));
        if (reputations.removeIf(reputation -> reputation.faction() == faction)) {
            player.setAttached(WarbandAttachments.ILLAGER_REPUTATION, List.copyOf(reputations));
        }
    }

    private static boolean inFactionSeat(Mob mob) {
        return mob.level() instanceof ServerLevel level
                && StructureCompat.inFactionSeat(level, mob.blockPosition());
    }

    private static void recordWitnesses(ServerLevel level, Mob victim, ServerPlayer player,
                                        boolean includeVictim, boolean relaxNotable) {
        long now = level.getGameTime();
        long originPos = victim.blockPosition().asLong();
        String originDimension = level.dimension().toString();
        AABB box = AABB.ofSize(victim.position(), WITNESS_RADIUS * 2.0, WITNESS_RADIUS, WITNESS_RADIUS * 2.0);
        // First pass: every eligible illager in range, without the notability filter,
        // so we can apply the "crowd remembers" rule based on the raw witness count.
        List<Mob> candidates = level.getEntitiesOfClass(Mob.class, box, mob ->
                mob.isAlive()
                        && MobData.isStamped(mob)
                        && IllagerInvasionCompat.isIllagerLike(mob)
                        && !isGrudgeSpawned(mob)
                        && (includeVictim || mob != victim)
                        && (mob == victim || mob.hasLineOfSight(victim) || mob.distanceToSqr(victim) < 12.0 * 12.0));

        // A crowd (3+) remembers any kill; otherwise only notable mobs / banner-carriers
        // form grudges. relaxNotable also skips the filter (used for the player's first
        // factioned kill, so the system always introduces itself at least once).
        List<Mob> witnesses;
        if (relaxNotable || candidates.size() >= CROWD_WITNESS_THRESHOLD) {
            witnesses = candidates;
        } else {
            witnesses = new ArrayList<>();
            for (Mob candidate : candidates) {
                if (isNotable(candidate) || carriesFactionBanner(candidate)) {
                    witnesses.add(candidate);
                }
            }
        }

        for (Mob witness : witnesses) {
            if (witness == victim && witness.isDeadOrDying()) continue;
            String name = witness.hasCustomName() ? witness.getCustomName().getString() : "Pillager";
            PENDING.put(witness.getUUID(), new PendingSurvivor(
                    player.getUUID(),
                    name,
                    IllagerFactionSystem.factionOrDefault(witness),
                    Math.max(MobData.get(witness).difficulty(), MobData.get(victim).difficulty()),
                    now + SURVIVE_CONFIRM_TICKS,
                    now + SURVIVOR_EXPIRY_TICKS,
                    originPos,
                    originDimension
            ));
        }
    }

    private static void tick(MinecraftServer server) {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        long now = server.overworld().getGameTime();
        confirmSurvivors(server, now);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            maybeLaunchRevenge(player, now);
            maybeLaunchBountyHunter(player, now);
            maybeLaunchVengeance(player, now);
        }
    }

    private static void confirmSurvivors(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, PendingSurvivor>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingSurvivor pending = iterator.next().getValue();
            if (now < pending.confirmAt) continue;

            // The witness was not killed inside the confirm window, it got away.
            // Form the grudge on the timer alone; do NOT require the mob to still
            // be loaded. (afterDeath removes killed witnesses from PENDING, and a
            // chunk unload would otherwise silently drop every grudge.)
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
            if (player != null) {
                addGrudge(player, pending);
                iterator.remove();
            } else if (now > pending.expiresAt) {
                iterator.remove();
            }
        }
    }

    private static void addGrudge(ServerPlayer player, PendingSurvivor survivor) {
        long readyAt = player.level().getGameTime() + REVENGE_DELAY_TICKS;
        List<IllagerGrudge> grudges = new ArrayList<>(grudges(player));
        for (int i = 0; i < grudges.size(); i++) {
            IllagerGrudge existing = grudges.get(i);
            if (existing.survivorName().equals(survivor.name) && existing.faction() == survivor.faction) {
                grudges.set(i, existing.addAnger(20, Math.min(existing.readyAt(), readyAt)));
                player.setAttached(WarbandAttachments.ILLAGER_GRUDGES, trim(grudges));
                addReputation(player, survivor.faction, 10, readyAt + BOUNTY_RETRY_TICKS);
                return;
            }
        }
        grudges.add(new IllagerGrudge(survivor.name, survivor.faction, survivor.difficulty, 40, readyAt, 0,
                survivor.originPos, survivor.originDimension));
        player.setAttached(WarbandAttachments.ILLAGER_GRUDGES, trim(grudges));
        addReputation(player, survivor.faction, 20, readyAt + BOUNTY_RETRY_TICKS);
        // "Slipped away" introduces a named survivor — keep this in chat, not action bar.
        player.sendSystemMessage(Component.literal(
                survivor.name + " slipped away, the " + survivor.faction.displayName()
                        + " will remember this."));
        WarbandCriteria.fire(player, WarbandCriteria.FIRST_GRUDGE);
    }

    private static void maybeLaunchRevenge(ServerPlayer player, long now) {
        if (!WarbandConfig.illagerGrudgesEnabled) return;
        List<IllagerGrudge> grudges = new ArrayList<>(grudges(player));
        if (grudges.isEmpty()) return;

        for (int i = 0; i < grudges.size(); i++) {
            IllagerGrudge grudge = grudges.get(i);
            if (grudge.readyAt() > now || grudge.attempts() >= 3) continue;
            if (player.getRandom().nextFloat() > 0.35f) {
                // Random gate miss — reschedule without burning an attempt, otherwise
                // unlucky rolls can quietly retire a grudge in ~9 minutes with no patrol spawned.
                grudges.set(i, new IllagerGrudge(grudge.survivorName(), grudge.faction(), grudge.difficulty(),
                        grudge.anger(), now + RETRY_DELAY_TICKS, grudge.attempts(),
                        grudge.originPos(), grudge.originDimension()));
                break;
            }
            List<IllagerGrudge> party = readyFactionGrudges(grudges, grudge.faction(), now);
            if (spawnRevengePatrol(player, party)) {
                grudges.removeIf(candidate -> party.contains(candidate));
            } else {
                for (int j = 0; j < grudges.size(); j++) {
                    IllagerGrudge candidate = grudges.get(j);
                    if (party.contains(candidate)) {
                        grudges.set(j, candidate.attempted(now + RETRY_DELAY_TICKS));
                    }
                }
            }
            break;
        }
        player.setAttached(WarbandAttachments.ILLAGER_GRUDGES, trim(grudges));
    }

    public static boolean debugSpawnRevengeParty(ServerPlayer player, double difficulty) {
        IllagerFaction faction = IllagerFaction.pick(player.getUUID().hashCode() + player.getBlockX() * 31 + player.getBlockZ());
        IllagerGrudge grudge = new IllagerGrudge("Debug Captain", faction, (float) difficulty, 60, 0, 0,
                player.blockPosition().asLong(), player.level().dimension().toString());
        return spawnRevengePatrol(player, List.of(grudge));
    }

    public static List<String> intelLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        List<IllagerGrudge> grudges = grudges(player);
        List<FactionReputation> reputations = reputations(player);
        if (grudges.isEmpty() && reputations.isEmpty()) {
            return List.of("No faction intel on you yet. Kill a factioned illager and they will start to take notice.");
        }
        if (!reputations.isEmpty()) {
            lines.add("Faction heat:");
            for (FactionReputation reputation : reputations) {
                lines.add("  " + reputation.faction().displayName() + ": " + reputation.heat()
                        + " (" + reputation.state().label() + ")");
            }
        }
        if (!grudges.isEmpty()) {
            lines.add("Known survivors:");
            for (IllagerGrudge grudge : grudges) {
                lines.add("  " + grudge.survivorName() + " / " + grudge.faction().displayName()
                        + " anger " + grudge.anger() + " attempts " + grudge.attempts());
            }
        } else if (!reputations.isEmpty()) {
            lines.add("No named survivors yet — no notable illagers escaped to remember you.");
        }
        return lines;
    }

    private static boolean spawnRevengePatrol(ServerPlayer player, List<IllagerGrudge> grudges) {
        if (grudges.isEmpty()) return false;
        IllagerFaction faction = grudges.getFirst().faction();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = findRevengeSpawn(level, player, grudges.getFirst());
        if (origin == null) return false;

        double difficulty = Math.max(0.45, maxDifficulty(grudges));
        int anger = totalAnger(grudges);
        int size = Math.min(7 + MultiplayerDirector.revengePartyBonus(level, player.blockPosition()),
                Math.max(grudges.size(), 2 + anger / 35 + (int) Math.floor(difficulty * 2.0)
                        + MultiplayerDirector.revengePartyBonus(level, player.blockPosition())));
        List<Mob> spawned = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockPos pos = origin.offset(player.getRandom().nextInt(7) - 3, 0, player.getRandom().nextInt(7) - 3);
            Mob mob = spawnMember(level, pos, difficulty, i);
            if (mob == null) continue;
            IllagerFactionSystem.setFaction(mob, faction);
            markGrudgeSpawned(mob);
            mob.setTarget(player);
            spawned.add(mob);
        }
        if (spawned.isEmpty()) return false;

        SquadCoordinator.createSquad(level, spawned, difficulty);
        for (int i = 0; i < spawned.size() && i < grudges.size(); i++) {
            Mob returned = spawned.get(i);
            returned.setCustomName(Component.literal(returnedName(grudges.get(i).survivorName())));
            returned.setCustomNameVisible(true);
        }
        for (Mob mob : spawned) {
            mob.setTarget(player);
        }

        maybeSpawnRivalInterception(player, grudges.getFirst(), origin, difficulty, spawned);
        TacticalEffects.arrivalCue(level, origin.getCenter(), TacticalEffects.ArrivalCue.REVENGE);
        player.sendSystemMessage(Component.literal("Familiar horns answer from the " + faction.displayName() + "."), true);
        WarbandCriteria.fire(player, WarbandCriteria.FIRST_REVENGE);
        return true;
    }

    private static void maybeLaunchBountyHunter(ServerPlayer player, long now) {
        if (!WarbandConfig.illagerBountyHuntersEnabled || !WarbandConfig.illagerFactionsEnabled) return;
        List<FactionReputation> reputations = new ArrayList<>(reputations(player));
        if (reputations.isEmpty()) return;

        for (int i = 0; i < reputations.size(); i++) {
            FactionReputation reputation = reputations.get(i);
            if (reputation.heat() < BOUNTY_HEAT_THRESHOLD || reputation.bountyReadyAt() > now) continue;
            if (spawnBountyHunter(player, reputation)) {
                reputations.set(i, reputation.coolDown(now + BOUNTY_RETRY_TICKS * 2L));
            } else {
                reputations.set(i, new FactionReputation(reputation.faction(), reputation.heat(),
                        now + BOUNTY_RETRY_TICKS, reputation.warReadyAt(), reputation.crusadeReadyAt()));
            }
            player.setAttached(WarbandAttachments.ILLAGER_REPUTATION, trimReputation(reputations));
            return;
        }
    }

    /**
     * Unprompted faction action driven purely by escalation state: WAR triggers periodic
     * field patrols, CRUSADE triggers mansion-led assaults near the player's home/respawn.
     * Both have long per-faction cooldowns so a player at max heat is harassed, not flooded.
     */
    private static void maybeLaunchVengeance(ServerPlayer player, long now) {
        if (!WarbandConfig.illagerFactionsEnabled || !WarbandConfig.illagerGrudgesEnabled) return;
        List<FactionReputation> reputations = new ArrayList<>(reputations(player));
        if (reputations.isEmpty()) return;

        boolean mutated = false;
        for (int i = 0; i < reputations.size(); i++) {
            FactionReputation reputation = reputations.get(i);
            VengeanceState state = reputation.state();
            if (state == VengeanceState.CRUSADE && reputation.crusadeReadyAt() <= now) {
                if (spawnCrusade(player, reputation)) {
                    // Keep the faction in at least WAR after a crusade — a single
                    // assault should not reset months of player-faction conflict.
                    int reducedHeat = Math.max(80, reputation.heat() - CRUSADE_HEAT_RELIEF);
                    reputations.set(i, new FactionReputation(reputation.faction(), reducedHeat,
                            reputation.bountyReadyAt(),
                            now + WAR_PATROL_COOLDOWN_TICKS,
                            now + CRUSADE_COOLDOWN_TICKS));
                } else {
                    reputations.set(i, reputation.withCrusadeReady(now + CRUSADE_COOLDOWN_TICKS / 4));
                }
                mutated = true;
                break;
            }
            if (state == VengeanceState.WAR && reputation.warReadyAt() <= now) {
                if (spawnWarPatrol(player, reputation)) {
                    reputations.set(i, reputation.withWarReady(now + WAR_PATROL_COOLDOWN_TICKS));
                } else {
                    reputations.set(i, reputation.withWarReady(now + WAR_PATROL_COOLDOWN_TICKS / 4));
                }
                mutated = true;
                break;
            }
        }
        if (mutated) {
            player.setAttached(WarbandAttachments.ILLAGER_REPUTATION, trimReputation(reputations));
        }
    }

    private static boolean spawnWarPatrol(ServerPlayer player, FactionReputation reputation) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = findSpawnPos(level, player.getRandom(), player.getBlockX(), player.getBlockZ());
        if (origin == null) return false;

        double difficulty = Math.min(1.0, 0.55 + reputation.heat() / 400.0);
        int size = 3 + player.getRandom().nextInt(2)
                + MultiplayerDirector.revengePartyBonus(level, player.blockPosition());
        List<Mob> spawned = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockPos pos = origin.offset(player.getRandom().nextInt(7) - 3, 0, player.getRandom().nextInt(7) - 3);
            Mob mob = spawnMember(level, pos, difficulty, i);
            if (mob == null) continue;
            IllagerFactionSystem.setFaction(mob, reputation.faction());
            markGrudgeSpawned(mob);
            mob.setTarget(player);
            spawned.add(mob);
        }
        if (spawned.isEmpty()) return false;
        SquadCoordinator.createSquad(level, spawned, difficulty);
        TacticalEffects.arrivalCue(level, origin.getCenter(), TacticalEffects.ArrivalCue.WAR_PATROL);
        player.sendSystemMessage(Component.literal(
                "A war patrol of the " + reputation.faction().displayName() + " moves to find you."), true);
        WarbandCriteria.fire(player, WarbandCriteria.FACTION_AT_WAR);
        return true;
    }

    private static boolean spawnCrusade(ServerPlayer player, FactionReputation reputation) {
        ServerLevel level = (ServerLevel) player.level();
        // Crusades muster near the player's home (respawn point if set) for that
        // "they came for my base" beat; falls back to current position otherwise.
        BlockPos anchor = player.getRespawnConfig() != null
                && player.getRespawnConfig().respawnData().dimension().equals(level.dimension())
                ? player.getRespawnConfig().respawnData().pos()
                : player.blockPosition();
        BlockPos origin = findSpawnPos(level, player.getRandom(), anchor.getX(), anchor.getZ());
        if (origin == null) {
            origin = findSpawnPos(level, player.getRandom(), player.getBlockX(), player.getBlockZ());
        }
        if (origin == null) return false;

        double difficulty = Math.min(1.0, 0.85 + reputation.heat() / 800.0);
        int size = 6 + player.getRandom().nextInt(3)
                + MultiplayerDirector.revengePartyBonus(level, player.blockPosition()) * 2;
        List<Mob> spawned = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockPos pos = origin.offset(player.getRandom().nextInt(9) - 4, 0, player.getRandom().nextInt(9) - 4);
            Mob mob = spawnMember(level, pos, difficulty, i);
            if (mob == null) continue;
            IllagerFactionSystem.setFaction(mob, reputation.faction());
            markGrudgeSpawned(mob);
            mob.setTarget(player);
            spawned.add(mob);
        }
        if (spawned.isEmpty()) return false;
        SquadCoordinator.createSquad(level, spawned, difficulty);
        Mob leader = spawned.getFirst();
        leader.setCustomName(Component.literal("Crusade Captain of the " + reputation.faction().displayName()));
        leader.setCustomNameVisible(true);
        TacticalEffects.arrivalCue(level, origin.getCenter(), TacticalEffects.ArrivalCue.CRUSADE);
        player.sendSystemMessage(Component.literal(
                "A crusade of the " + reputation.faction().displayName() + " has come for you."), true);
        WarbandCriteria.fire(player, WarbandCriteria.CRUSADE_CALLED);
        return true;
    }

    private static boolean spawnBountyHunter(ServerPlayer player, FactionReputation reputation) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = findSpawnPos(level, player.getRandom(), player.getBlockX(), player.getBlockZ());
        if (origin == null) return false;

        double difficulty = Math.min(1.0, 0.75 + reputation.heat() / 500.0);
        Mob hunter = EntityType.PILLAGER.spawn(level, origin, EntitySpawnReason.EVENT);
        if (hunter == null) return false;
        IllagerFactionSystem.setFaction(hunter, reputation.faction());
        markGrudgeSpawned(hunter);
        SquadCoordinator.createSquad(level, List.of(hunter), difficulty);
        hunter.setCustomName(Component.literal("Bounty Hunter of the " + reputation.faction().displayName()));
        hunter.setCustomNameVisible(true);
        hunter.setTarget(player);
        // A relentless hunter, not a damage sponge: Speed to stay on the player
        // and Strength to bite, no Resistance. Its difficulty comes from the
        // squad AI and the chase, not from soaking hits. Lasts one encounter.
        int buffTicks = 20 * 60 * 2;
        hunter.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, buffTicks, 1, false, true));
        hunter.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, buffTicks, 0, false, true));
        TacticalEffects.arrivalCue(level, origin.getCenter(), TacticalEffects.ArrivalCue.BOUNTY);
        player.sendSystemMessage(Component.literal("A bounty hunter from the " + reputation.faction().displayName() + " has your trail."), true);
        WarbandCriteria.fire(player, WarbandCriteria.BOUNTY_SUMMONED);
        return true;
    }

    private static void maybeSpawnRivalInterception(ServerPlayer player, IllagerGrudge grudge,
                                                    BlockPos origin, double difficulty, List<Mob> patrol) {
        if (!WarbandConfig.illagerRivalriesEnabled || !WarbandConfig.illagerFactionsEnabled) return;
        if (patrol.isEmpty()) return;
        // No rival pile-on at evoker tier, that revenge patrol is already a handful.
        if (difficulty >= 0.85) return;
        if (player.getRandom().nextInt(100) >= RIVAL_INTERCEPT_CHANCE) return;

        ServerLevel level = (ServerLevel) player.level();
        IllagerFaction rival = grudge.faction().rival();
        // Self-rival factions (zealot orders) don't get intercepted.
        if (rival == grudge.faction()) return;
        double rivalDifficulty = Math.max(0.45, difficulty - 0.10);
        List<Mob> spawned = new ArrayList<>();
        int size = 1 + player.getRandom().nextInt(2);
        for (int i = 0; i < size; i++) {
            BlockPos pos = origin.offset(player.getRandom().nextInt(15) - 7, 0, player.getRandom().nextInt(15) - 7);
            Mob mob = spawnMember(level, pos, rivalDifficulty, i);
            if (mob == null) continue;
            IllagerFactionSystem.setFaction(mob, rival);
            markGrudgeSpawned(mob);
            // Rivals intercept the revenge patrol, not the player, a genuine
            // third party the player can turn to their advantage.
            mob.setTarget(patrol.get(player.getRandom().nextInt(patrol.size())));
            spawned.add(mob);
        }
        if (spawned.isEmpty()) return;
        SquadCoordinator.createSquad(level, spawned, rivalDifficulty);
        Mob leader = spawned.getFirst();
        leader.setCustomName(Component.literal("Rival Scout of the " + rival.displayName()));
        leader.setCustomNameVisible(true);
        player.sendSystemMessage(Component.literal(
                "Rivals from the " + rival.displayName() + " move to intercept the "
                        + grudge.faction().displayName() + "."), true);
    }

    private static Mob spawnMember(ServerLevel level, BlockPos pos, double difficulty, int index) {
        EntityType<? extends Mob> type = EntityType.PILLAGER;
        if (difficulty >= 0.70 && index == 1) type = EntityType.VINDICATOR;
        if (difficulty >= 0.85 && index == 2) type = EntityType.EVOKER;
        return type.spawn(level, pos, EntitySpawnReason.EVENT);
    }

    /**
     * Where a revenge patrol musters: near the grudge's recorded origin when the
     * player is still close to it (revenge returns to the scene), otherwise near
     * the player as a fallback. Origin is a plain coordinate, so this works with
     * any pillager structure, vanilla or modded.
     */
    private static BlockPos findRevengeSpawn(ServerLevel level, ServerPlayer player, IllagerGrudge grudge) {
        if (grudge.hasOrigin() && grudge.originDimension().equals(level.dimension().toString())) {
            BlockPos origin = BlockPos.of(grudge.originPos());
            double dx = origin.getX() + 0.5 - player.getX();
            double dz = origin.getZ() + 0.5 - player.getZ();
            if (dx * dx + dz * dz < (double) ORIGIN_MUSTER_RANGE * ORIGIN_MUSTER_RANGE) {
                BlockPos atOrigin = findSpawnPos(level, player.getRandom(), origin.getX(), origin.getZ());
                if (atOrigin != null) return atOrigin;
            }
        }
        return findSpawnPos(level, player.getRandom(), player.getBlockX(), player.getBlockZ());
    }

    private static BlockPos findSpawnPos(ServerLevel level, RandomSource random, int centerX, int centerZ) {
        for (int attempts = 0; attempts < 16; attempts++) {
            int distance = SPAWN_MIN_DISTANCE + random.nextInt(SPAWN_RANGE);
            double angle = random.nextDouble() * Math.PI * 2.0;
            int x = centerX + (int) Math.round(Math.cos(angle) * distance);
            int z = centerZ + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getWorldBorder().isWithinBounds(pos)
                    && level.noCollision(EntityType.PILLAGER.getSpawnAABB(pos.getX(), pos.getY(), pos.getZ()))) {
                return pos;
            }
        }
        return null;
    }

    /** A grudge-worthy illager: a squad leader, or one at notable difficulty. */
    private static boolean isNotable(Mob mob) {
        MobData data = MobData.get(mob);
        return data.role() == Role.LEADER || data.difficulty() >= 0.65f;
    }

    /** Banner-bearing illagers represent the faction directly; their kills always register. */
    private static boolean carriesFactionBanner(Mob mob) {
        return mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).getItem()
                instanceof net.minecraft.world.item.BannerItem;
    }

    private static void markGrudgeSpawned(Mob mob) {
        mob.setAttached(WarbandAttachments.ILLAGER_GRUDGE_SPAWNED, true);
    }

    private static boolean isGrudgeSpawned(Mob mob) {
        return Boolean.TRUE.equals(mob.getAttached(WarbandAttachments.ILLAGER_GRUDGE_SPAWNED));
    }

    private static List<IllagerGrudge> readyFactionGrudges(List<IllagerGrudge> grudges, IllagerFaction faction, long now) {
        List<IllagerGrudge> ready = new ArrayList<>();
        for (IllagerGrudge grudge : grudges) {
            if (grudge.faction() == faction && grudge.readyAt() <= now && grudge.attempts() < 3) {
                ready.add(grudge);
            }
        }
        return ready;
    }

    private static float maxDifficulty(List<IllagerGrudge> grudges) {
        float max = 0.0f;
        for (IllagerGrudge grudge : grudges) {
            max = Math.max(max, grudge.difficulty());
        }
        return max;
    }

    private static int totalAnger(List<IllagerGrudge> grudges) {
        int total = 0;
        for (IllagerGrudge grudge : grudges) {
            total += grudge.anger();
        }
        return total;
    }

    private static String returnedName(String survivorName) {
        String clean = survivorName;
        int factionIndex = clean.indexOf(" of the ");
        if (factionIndex >= 0) {
            clean = clean.substring(0, factionIndex);
        }
        String[] parts = clean.trim().split("\\s+");
        String personalName = parts.length == 0 ? "Survivor" : parts[parts.length - 1];
        return personalName + " the Returned";
    }

    private static void addReputation(ServerPlayer player, IllagerFaction faction, int heat, long bountyReadyAt) {
        // Heat is also the display value for /warband intel — track it regardless
        // of whether bounty hunters are enabled; only the hunter spawn is gated.
        List<FactionReputation> reputations = new ArrayList<>(reputations(player));
        for (int i = 0; i < reputations.size(); i++) {
            FactionReputation reputation = reputations.get(i);
            if (reputation.faction() == faction) {
                reputations.set(i, reputation.addHeat(heat, bountyReadyAt));
                player.setAttached(WarbandAttachments.ILLAGER_REPUTATION, trimReputation(reputations));
                return;
            }
        }
        reputations.add(new FactionReputation(faction, heat, bountyReadyAt));
        player.setAttached(WarbandAttachments.ILLAGER_REPUTATION, trimReputation(reputations));
    }

    private static List<ServerPlayer> participantsNear(ServerLevel level, Mob mob, ServerPlayer source) {
        if (!WarbandConfig.multiplayerFeaturesEnabled) return List.of(source);
        AABB box = AABB.ofSize(mob.position(), 64.0 * 2.0, 32.0, 64.0 * 2.0);
        List<ServerPlayer> participants = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, box,
                player -> player.isAlive() && !player.isSpectator()));
        if (!participants.contains(source)) {
            participants.add(source);
        }
        return participants;
    }

    private static List<IllagerGrudge> grudges(ServerPlayer player) {
        List<IllagerGrudge> grudges = player.getAttached(WarbandAttachments.ILLAGER_GRUDGES);
        return grudges != null ? grudges : List.of();
    }

    private static List<FactionReputation> reputations(ServerPlayer player) {
        List<FactionReputation> reputations = player.getAttached(WarbandAttachments.ILLAGER_REPUTATION);
        return reputations != null ? reputations : List.of();
    }

    private static List<IllagerGrudge> trim(List<IllagerGrudge> grudges) {
        if (grudges.size() <= MAX_GRUDGES_PER_PLAYER) return List.copyOf(grudges);
        return List.copyOf(grudges.subList(grudges.size() - MAX_GRUDGES_PER_PLAYER, grudges.size()));
    }

    private static List<FactionReputation> trimReputation(List<FactionReputation> reputations) {
        if (reputations.size() <= MAX_REPUTATION_RECORDS) return List.copyOf(reputations);
        return List.copyOf(reputations.subList(reputations.size() - MAX_REPUTATION_RECORDS, reputations.size()));
    }

    private record PendingSurvivor(UUID playerId, String name, IllagerFaction faction,
                                   float difficulty, long confirmAt, long expiresAt,
                                   long originPos, String originDimension) {
    }
}
