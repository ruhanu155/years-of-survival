package com.yearofice.api.world;

import net.minecraft.server.level.ServerLevel;

/**
 * The one door for permanent terrain edits from every producer (burn, erosion, dams, thaw slumps, frost heave).
 * Owner: {@code world.terrain}. The op is applied within the next game hour to a loaded chunk, or when the chunk
 * next loads (Catch-up on chunk load); a block a player placed is never changed.
 */
public interface WorldEditService {
    /** Queues {@code op} in its chunk's history. */
    void submit(ServerLevel level, ChangeOp op);

    /** Ops queued for the chunk at {@code chunkX, chunkZ}. */
    int pending(ServerLevel level, int chunkX, int chunkZ);

    static WorldEditService get() {
        WorldEditService s = Holder.service;
        if (s == null) throw new IllegalStateException("WorldEditService not installed yet");
        return s;
    }

    static void install(WorldEditService s) { Holder.service = s; }

    final class Holder {
        private static volatile WorldEditService service;

        private Holder() {}
    }
}
