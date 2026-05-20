package com.warband.command;

import com.mojang.brigadier.context.CommandContext;
import com.warband.config.WarbandConfig;
import com.warband.difficulty.DifficultyManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /warband} command. Phase 1 ships {@code /warband difficulty}; more
 * subcommands (notably {@code /warband debug}) arrive with later phases.
 */
public final class WarbandCommand {

    private WarbandCommand() {
    }

    /** Register the command tree. Called from {@code onInitialize}. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("warband")
                        .then(Commands.literal("difficulty")
                                .executes(WarbandCommand::reportDifficulty))));
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
        return 1;
    }
}
