package com.yearofice.world.snow;

import com.yearofice.sim.world.SnowPack;
import com.yearofice.sim.world.SnowPack.SnowClass;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.world.season.PassContext;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The snow pass (Snow cover and permafrost blocks): for each column sets the layer count to the day's depth for its
 * snow class, rounded to the layer, stacking a second and third {@code snow_cover} on a full block under it. Writes
 * only columns whose count changed; never a masked block, never a plant (a column whose cell above the ground holds
 * one is left bare); snow does not lie on water, skim ice or an ice hole.
 */
public final class SnowPass {
    private SnowPass() {}

    /**
     * The snow class of a column (fixed per column in the blueprint; derived here from the ground heights, which do
     * not change with snow): under a canopy; in the lee of ground two or more blocks higher to the west; on a slope
     * facing south (ground two or more blocks higher to the north than to the south); else open.
     * ponytail: canopy instead of "within 2 blocks of a spruce trunk", chunk-edge columns clamp to the chunk —
     * upgrade: read the generation tree list into the season-stamp attachment.
     */
    public static SnowClass classify(PassContext c, int lx, int lz) {
        int top = c.chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
        int by = c.base[PassContext.idx(lx, lz)];
        if (top > by) {
            BlockState t = c.chunk.getBlockState(new BlockPos(c.chunk.getPos().getMinBlockX() + lx, top, c.chunk.getPos().getMinBlockZ() + lz));
            if (t.is(BlockTags.LEAVES) || t.getBlock() instanceof com.yearofice.worldgen.block.YoiLeavesBlock) return SnowClass.UNDER_SPRUCE;
        }
        int west = c.base[PassContext.idx(Math.max(0, lx - 3), lz)];
        if (west - by >= 2) return SnowClass.LEE_DRIFT;
        int north = c.base[PassContext.idx(lx, Math.max(0, lz - 2))], south = c.base[PassContext.idx(lx, Math.min(15, lz + 2))];
        if (north - south >= 2) return SnowClass.SOUTH_SLOPE;
        return SnowClass.OPEN;
    }

    public static void apply(PassContext c) {
        int x0 = c.chunk.getPos().getMinBlockX(), z0 = c.chunk.getPos().getMinBlockZ();
        var snowBlock = WorldBlocks.SNOW_COVER.get();
        int maxBlocks = (c.snow.maxLayers() + 7) / 8;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int by = c.base[PassContext.idx(lx, lz)];
                if (by < c.chunk.getMinBuildHeight()) continue;
                BlockPos ground = new BlockPos(x0 + lx, by, z0 + lz);
                BlockState gs = c.get(ground);
                boolean eligible = gs.getFluidState().isEmpty() && !gs.is(IceBlocks.SKIM_ICE.get()) && !gs.is(Blocks.WATER)
                        && gs.isFaceSturdy(c.chunk, ground, Direction.UP);
                SnowClass cls = classify(c, lx, lz);
                SnowPack.State st = c.core.snow(cls);
                int n = eligible ? SnowPack.layers(st.depthCm(), c.snow) : 0;
                for (int k = 0; k < maxBlocks; k++) {
                    m.set(x0 + lx, by + 1 + k, z0 + lz);
                    BlockState cur = c.get(m);
                    int layers = Math.max(0, Math.min(8, n - 8 * k));
                    BlockState want = layers == 0 ? Blocks.AIR.defaultBlockState()
                            : snowBlock.defaultBlockState().setValue(SnowCoverBlock.LAYERS, layers)
                                    .setValue(SnowCoverBlock.CRUST, st.crust).setValue(SnowCoverBlock.WET, st.wet);
                    if (!cur.isAir() && !cur.is(snowBlock)) break; // a plant or a piece above the ground: leave the column
                    if (cur != want) c.set(m.immutable(), want);
                }
            }
        }
    }
}
