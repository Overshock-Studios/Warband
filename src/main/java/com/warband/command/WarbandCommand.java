package com.warband.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.warband.ai.Squad;
import com.warband.ai.SquadCoordinator;
import com.warband.config.WarbandConfig;
import com.warband.difficulty.DifficultyManager;
import com.warband.entity.MobData;
import com.warband.entity.WarbandAttachments;
import com.warband.illager.IllagerGrudgeSystem;
import com.warband.spawn.SpawnDirector;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The {@code /warband} command. Ships {@code /warband difficulty} and
 * {@code /warband mobs}; more subcommands arrive with later phases.
 */
public final class WarbandCommand {

    /** Edge length of the cube scanned by {@code /warband mobs}. */
    private static final double MOB_SCAN_SIZE = 64.0;

    private WarbandCommand() {
    }

    /** Register the command tree. Called from {@code onInitialize}. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("warband")
                        .then(Commands.literal("difficulty")
                                .executes(WarbandCommand::reportDifficulty))
                        .then(Commands.literal("mobs")
                                .executes(WarbandCommand::reportMobs))
                        .then(Commands.literal("debug")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("spawn")
                                        .then(Commands.argument("difficulty", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(WarbandCommand::debugSpawn)))
                                .then(Commands.literal("squad")
                                        .then(Commands.argument("difficulty", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(WarbandCommand::debugSquad)))
                                .then(Commands.literal("revenge")
                                        .then(Commands.argument("difficulty", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(WarbandCommand::debugRevenge))))));
    }

    private static int reportDifficulty(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        ServerPlayer player = source.getPlayer();

        double difficulty = DifficultyManager.getDifficulty(level, pos, player);

        source.sendSuccess(() -> Component.literal(
                String.format("[Warband] Local difficulty: %.2f", difficulty)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  mode=%s  vanilla=%s",
                WarbandConfig.difficultyMode,
                level.getLevelData().getDifficulty().getSerializedName())), false);
        if (player != null) {
            Float score = player.getAttached(WarbandAttachments.PLAYER_SCORE);
            source.sendSuccess(() -> Component.literal(String.format(
                    "  your power=%.2f", score != null ? score : 0.0f)), false);
        }
        return 1;
    }

    /** Spawns a zombie at the source position, stamped at the given difficulty. */
    private static int debugSpawn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        double difficulty = DoubleArgumentType.getDouble(ctx, "difficulty");
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        Zombie zombie = EntityType.ZOMBIE.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (zombie == null) {
            source.sendFailure(Component.literal("[Warband] Failed to spawn debug zombie."));
            return 0;
        }
        SpawnDirector.stamp(zombie, difficulty);

        double health = zombie.getMaxHealth();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Warband] Spawned zombie at difficulty %.2f — max health %.1f (vanilla 20.0)",
                difficulty, health)), false);
        return 1;
    }

    /** Spawns a small zombie squad at the source position, stamped with roles. */
    private static int debugSquad(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        double difficulty = DoubleArgumentType.getDouble(ctx, "difficulty");
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        Squad squad = SquadCoordinator.createDebugSquad(level, pos, difficulty);
        int count = squad.members().size();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Warband] Spawned squad %d with %d mob(s) at difficulty %.2f",
                squad.id(), count, difficulty)), false);
        return count;
    }

    /** Forces an illager revenge party against the executing player. */
    private static int debugRevenge(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("[Warband] Revenge debug requires a player source."));
            return 0;
        }

        double difficulty = DoubleArgumentType.getDouble(ctx, "difficulty");
        if (!IllagerGrudgeSystem.debugSpawnRevengeParty(player, difficulty)) {
            source.sendFailure(Component.literal("[Warband] Failed to find a revenge-party spawn position."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(
                "[Warband] Spawned debug revenge party at difficulty %.2f", difficulty)), false);
        return 1;
    }

    /** Reports Warband-stamped mobs within {@link #MOB_SCAN_SIZE} blocks. */
    private static int reportMobs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), MOB_SCAN_SIZE, MOB_SCAN_SIZE, MOB_SCAN_SIZE);
        List<Mob> stamped = level.getEntitiesOfClass(Mob.class, box, MobData::isStamped);

        if (stamped.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "[Warband] No stamped mobs within " + (int) MOB_SCAN_SIZE + " blocks."), false);
            return 0;
        }

        double min = 1.0;
        double max = 0.0;
        double sum = 0.0;
        int squadded = 0;
        for (Mob mob : stamped) {
            MobData data = MobData.get(mob);
            double d = data.difficulty();
            min = Math.min(min, d);
            max = Math.max(max, d);
            sum += d;
            if (data.inSquad()) {
                squadded++;
            }
        }
        double avg = sum / stamped.size();

        int count = stamped.size();
        double fMin = min;
        double fMax = max;
        int fSquadded = squadded;
        int activeSquads = SquadCoordinator.activeSquads();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Warband] %d stamped mob(s) nearby — %d in squads, %d active squad(s) — difficulty min %.2f / avg %.2f / max %.2f",
                count, fSquadded, activeSquads, fMin, avg, fMax)), false);
        return count;
    }
}
