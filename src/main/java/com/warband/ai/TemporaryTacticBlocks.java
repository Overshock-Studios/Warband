package com.warband.ai;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tracks temporary blocks placed by Warband tactics and removes them later. */
public final class TemporaryTacticBlocks {

    private static final int BLOCK_UPDATE = 3;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private TemporaryTacticBlocks() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ENTRIES.isEmpty()) return;

            Iterator<Entry> iterator = ENTRIES.iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (entry.level.getGameTime() < entry.expiresAt) continue;

                if (entry.level.hasChunk(entry.pos.getX() >> 4, entry.pos.getZ() >> 4)
                        && entry.level.getBlockState(entry.pos).is(entry.block)) {
                    entry.level.setBlock(entry.pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE);
                }
                iterator.remove();
            }
        });
    }

    public static boolean place(ServerLevel level, BlockPos pos, Block block, int ttlTicks) {
        return place(level, pos, block.defaultBlockState(), ttlTicks);
    }

    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ttlTicks) {
        if (!WarbandConfig.temporaryTacticBlocks) return false;
        if (!level.getBlockState(pos).isAir()) return false;

        BlockPos immutable = pos.immutable();
        if (!level.setBlock(immutable, state, BLOCK_UPDATE)) return false;
        ENTRIES.add(new Entry(level, immutable, state.getBlock(), level.getGameTime() + ttlTicks));
        if (state.is(Blocks.COBWEB)) {
            TacticalEffects.web(level, immutable);
        }
        return true;
    }

    public static boolean freezeWater(ServerLevel level, BlockPos waterPos, int ttlTicks) {
        if (!WarbandConfig.temporaryTacticBlocks) return false;
        if (!level.getBlockState(waterPos).is(Blocks.WATER)) return false;

        BlockPos immutable = waterPos.immutable();
        if (!level.setBlock(immutable, Blocks.FROSTED_ICE.defaultBlockState(), BLOCK_UPDATE)) return false;
        ENTRIES.add(new Entry(level, immutable, Blocks.FROSTED_ICE, level.getGameTime() + ttlTicks));
        TacticalEffects.frost(level, immutable);
        return true;
    }

    private record Entry(ServerLevel level, BlockPos pos, Block block, long expiresAt) {
    }
}
