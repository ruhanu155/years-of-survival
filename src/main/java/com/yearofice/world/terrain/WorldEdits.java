package com.yearofice.world.terrain;

import com.yearofice.api.world.ChangeOp;
import com.yearofice.api.world.WorldEditService;
import net.minecraft.server.level.ServerLevel;

/** {@link WorldEditService}: the queue's front door. */
public final class WorldEdits implements WorldEditService {
    @Override
    public void submit(ServerLevel level, ChangeOp op) { TerrainChangeQueue.get(level).submit(op); }

    @Override
    public int pending(ServerLevel level, int chunkX, int chunkZ) {
        return TerrainChangeQueue.get(level).pending(new net.minecraft.world.level.ChunkPos(chunkX, chunkZ));
    }
}
