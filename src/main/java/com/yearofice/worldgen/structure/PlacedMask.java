package com.yearofice.worldgen.structure;

import com.yearofice.world.attach.PlacedMaskData;
import com.yearofice.world.attach.WorldAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * The {@code placed_mask} (Layer 9: World edits): one bit per block a player or a set-piece template placed. World
 * edits and the terrain-change queue (M7–M9) ask {@link #mayChange} before they touch a block, so erosion, floods
 * and slumps never wear a set piece or a player's work away. Kept per chunk in the {@code yearofice:placed_mask}
 * attachment (RECONCILE 167; it was SavedData {@code yearofice_placed_mask} at M1).
 */
public final class PlacedMask {
    private final LevelAccessor level;

    private PlacedMask(LevelAccessor level) { this.level = level; }

    /** The mask of a level, or of a generation region (only its own chunks may be asked). */
    public static PlacedMask get(LevelAccessor level) { return new PlacedMask(level); }

    private ChunkAccess chunk(BlockPos p) { return level.getChunk(p.getX() >> 4, p.getZ() >> 4, ChunkStatus.EMPTY, true); }

    private static PlacedMaskData data(ChunkAccess c) { return c.getData(WorldAttachments.PLACED_MASK); }

    public void mark(BlockPos pos) {
        ChunkAccess c = chunk(pos);
        if (data(c).mark(pos.getX(), pos.getY(), pos.getZ())) c.setUnsaved(true);
    }

    public void unmark(BlockPos pos) {
        ChunkAccess c = chunk(pos);
        PlacedMaskData d = c.getExistingDataOrNull(WorldAttachments.PLACED_MASK);
        if (d != null && d.clear(pos.getX(), pos.getY(), pos.getZ())) c.setUnsaved(true);
    }

    public boolean isPlaced(BlockPos pos) {
        PlacedMaskData d = chunk(pos).getExistingDataOrNull(WorldAttachments.PLACED_MASK);
        return d != null && d.isPlaced(pos.getX(), pos.getY(), pos.getZ());
    }

    /** False for any block a player or a set piece placed: terrain processes must leave it alone. */
    public boolean mayChange(BlockPos pos) { return !isPlaced(pos); }

    /** Placed blocks in a box. */
    public int count(BlockPos min, BlockPos max) {
        int c = 0;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) if (isPlaced(p)) c++;
        return c;
    }

    /** Forgets a box (debug and GameTest clean-up only). */
    public void clear(BlockPos min, BlockPos max) {
        for (BlockPos p : BlockPos.betweenClosed(min, max)) unmark(p);
    }
}
