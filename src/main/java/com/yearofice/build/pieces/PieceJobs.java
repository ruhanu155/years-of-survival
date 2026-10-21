package com.yearofice.build.pieces;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Queued piece-placing jobs (stamped structures): FIFO per level, a per-tick budget, the world edit done on the server thread. */
public final class PieceJobs {
    private PieceJobs() {}

    public record Job(BlockPos pos, BlockState state) {}

    private static final Map<ServerLevel, ArrayDeque<Job>> QUEUES = new ConcurrentHashMap<>();

    static void init() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            for (var en : QUEUES.entrySet()) {
                ArrayDeque<Job> q = en.getValue();
                for (int i = 0; i < PiecesConfig.stampPerTick() && !q.isEmpty(); i++) {
                    Job j = q.poll();
                    en.getKey().setBlock(j.pos(), j.state(), 3);
                }
            }
            QUEUES.values().removeIf(ArrayDeque::isEmpty);
        });
    }

    public static void queue(ServerLevel level, BlockPos pos, BlockState state) {
        QUEUES.computeIfAbsent(level, k -> new ArrayDeque<>()).add(new Job(pos.immutable(), state));
    }

    public static int pending(ServerLevel level) { ArrayDeque<Job> q = QUEUES.get(level); return q == null ? 0 : q.size(); }
}
