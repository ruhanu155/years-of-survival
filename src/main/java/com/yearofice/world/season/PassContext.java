package com.yearofice.world.season;

import com.yearofice.sim.world.IceTuning;
import com.yearofice.sim.world.SeasonCore;
import com.yearofice.sim.world.SnowTuning;
import com.yearofice.world.attach.PlacedMaskData;
import com.yearofice.world.attach.WorldAttachments;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What the chunk passes (ice, flood, snow) share: the chunk, who writes, today's {@link SeasonCore}, the placed
 * mask, and each column's surface. {@code base} is the highest block that is not snow cover; {@code waterBase} also
 * skips the flood water this chunk placed (its flags), so a flood never reads as the river's own top. Passes are
 * idempotent: they compute the wanted block per column and write only what differs. Catch-up writes through the
 * chunk directly ({@code live == false}); live passes use {@code level.setBlock}.
 */
public final class PassContext {
    public final ServerLevel level;
    public final LevelChunk chunk;
    public final boolean live;
    public final SeasonCore core;
    public final SnowTuning snow;
    public final IceTuning ice;
    public final Set<Long> floodFlags = new HashSet<>();
    public final int[] base = new int[256], waterBase = new int[256];
    final PlacedMaskData mask;
    public int writes;

    public PassContext(ServerLevel level, LevelChunk chunk, boolean live, SeasonCore core, SnowTuning snow, IceTuning ice,
                       List<Long> flood) {
        this.level = level;
        this.chunk = chunk;
        this.live = live;
        this.core = core;
        this.snow = snow;
        this.ice = ice;
        this.floodFlags.addAll(flood);
        this.mask = chunk.getExistingDataOrNull(WorldAttachments.PLACED_MASK);
        prepare();
    }

    public static int idx(int lx, int lz) { return lz << 4 | lx; }

    private void prepare() {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz); // ChunkAccess.getHeight is the top block itself
                while (y >= minY && chunk.getBlockState(m.set(x0 + lx, y, z0 + lz)).is(com.yearofice.worldgen.block.WorldBlocks.SNOW_COVER.get())) y--;
                base[idx(lx, lz)] = y;
                int w = y;
                while (w >= minY && floodFlags.contains(m.set(x0 + lx, w, z0 + lz).asLong())) w--;
                waterBase[idx(lx, lz)] = w;
            }
        }
    }

    public BlockState get(BlockPos p) { return chunk.getBlockState(p); }

    public boolean masked(BlockPos p) { return mask != null && mask.isPlaced(p.getX(), p.getY(), p.getZ()); }

    public boolean inside(BlockPos p) { return !chunk.isOutsideBuildHeight(p) && (p.getX() >> 4) == chunk.getPos().x && (p.getZ() >> 4) == chunk.getPos().z; }

    /** Writes {@code s} at {@code p} unless it is already there, masked or out of the chunk. */
    public boolean set(BlockPos p, BlockState s) {
        if (!inside(p) || masked(p) || chunk.getBlockState(p) == s) return false;
        if (live) level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        else chunk.setBlockState(p, s, false);
        writes++;
        return true;
    }
}
