package com.yearofice.world.terrain;

import com.yearofice.api.world.ChangeOp;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.yearofice.world.attach.PlacedMaskData;
import com.yearofice.world.attach.WorldAttachments;
import com.yearofice.worldgen.block.WorldBlocks;

/**
 * Applies queued ops to one chunk, in canonical order, never to a block a player or a piece placed. Live writes go
 * through the level (clients are told); catch-up writes go through the {@link LevelChunk} directly, because calling
 * {@code level.setBlock} inside a chunk's own load can ask for that chunk again.
 */
public final class TerrainApply {
    private TerrainApply() {}

    /** Applies at most {@code budget} due ops; returns the ops consumed (applied or refused, both leave the queue). */
    public static List<ChangeOp> apply(ServerLevel level, LevelChunk chunk, TerrainChangeQueue q, long today, boolean live, int budget) {
        List<ChangeOp> done = new ArrayList<>();
        PlacedMaskData mask = chunk.getExistingDataOrNull(WorldAttachments.PLACED_MASK);
        for (ChangeOp op : q.due(chunk.getPos(), today)) {
            if (done.size() >= budget) break;
            done.add(op);
            BlockPos p = op.pos();
            if (chunk.isOutsideBuildHeight(p)) continue;
            if (mask != null && mask.isPlaced(p.getX(), p.getY(), p.getZ())) continue;
            BlockState cur = chunk.getBlockState(p);
            if (op.airOnly() && !cur.isAir() && !cur.is(WorldBlocks.SNOW_COVER.get())) continue;
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(op.block()));
            BlockState want = b.defaultBlockState();
            if (cur == want) continue;
            if (live) level.setBlock(p, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            else chunk.setBlockState(p, want, false);
        }
        q.done(chunk.getPos(), done);
        if (!done.isEmpty()) chunk.setUnsaved(true);
        return done;
    }
}
