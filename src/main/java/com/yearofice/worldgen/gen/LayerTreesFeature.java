package com.yearofice.worldgen.gen;

import com.yearofice.worldgen.Cover;
import com.yearofice.worldgen.TreePlan;
import com.yearofice.worldgen.TreeShapes;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code yearofice:layer_trees}: places the trees of {@link TreePlan} whose base lies in this chunk, from the
 * builders in {@link TreeShapes}. Listed in every biome once, so it runs once a chunk. Writes only into air (logs
 * may replace leaves), so neighbouring chunks' crowns overlap the same in any order. A tree stands only on dry
 * ground: the top block must be one of our soils or surfaces, with nothing above it.
 */
public final class LayerTreesFeature extends Feature<NoneFeatureConfiguration> {
    public LayerTreesFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator() instanceof YoiChunkGenerator gen) || gen.world() == null) return false;
        ValleyWorld w = gen.world();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        boolean any = false;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (TreePlan.Tree t : TreePlan.treesIn(w, x0, z0, x0 + 16, z0 + 16)) {
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, t.x(), t.z());
            if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, t.x(), t.z()) != y) continue; // under water
            BlockState below = level.getBlockState(mp.set(t.x(), y - 1, t.z()));
            if (!stands(below)) continue;
            Block log = switch (t.species()) {
                case Cover.BLACK_SPRUCE -> WorldBlocks.BLACK_SPRUCE_LOG.get();
                case Cover.BIRCH -> WorldBlocks.BIRCH_LOG.get();
                default -> WorldBlocks.WHITE_SPRUCE_LOG.get();
            };
            Block leaves = switch (t.species()) {
                case Cover.BLACK_SPRUCE -> WorldBlocks.BLACK_SPRUCE_LEAVES.get();
                case Cover.BIRCH -> WorldBlocks.BIRCH_LEAVES.get();
                default -> WorldBlocks.WHITE_SPRUCE_LEAVES.get();
            };
            final int by = y;
            TreeShapes.build(t, (dx, dy, dz, part) -> {
                int px = t.x() + dx, pz = t.z() + dz;
                int py = by + dy;
                if (part == TreeShapes.Part.FALLEN_X || part == TreeShapes.Part.FALLEN_Z) {
                    int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, px, pz);
                    if (Math.abs(gy - by) > 1 || level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, px, pz) != gy) return;
                    py = gy;
                }
                BlockPos p = new BlockPos(px, py, pz);
                BlockState here = level.getBlockState(p);
                BlockState put = switch (part) {
                    case LOG -> log.defaultBlockState();
                    case LEAVES -> leaves.defaultBlockState();
                    case STEM -> WorldBlocks.SHRUB_STEM.get().defaultBlockState();
                    case BRUSH -> WorldBlocks.WILLOW_BRUSH.get().defaultBlockState();
                    case STUMP -> WorldBlocks.STUMP.get().defaultBlockState();
                    case FALLEN_X -> WorldBlocks.FALLEN_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                    case FALLEN_Z -> WorldBlocks.FALLEN_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                };
                if (wins(put, here)) level.setBlock(p, put, Block.UPDATE_CLIENTS);
            });
            any = true;
        }
        return any;
    }

    /**
     * Whether a tree part may replace what is there: a stronger rank wins (wood over stumps over leaves over brush
     * over air) and equal ranks break by block id, so overlapping trees give the same blocks in any order.
     */
    static boolean wins(BlockState put, BlockState here) {
        int a = rank(put), b = rank(here);
        if (a != b) return a > b;
        if (b == 0 || put.getBlock() == here.getBlock()) return false;
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(put.getBlock()).toString()
                .compareTo(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(here.getBlock()).toString()) > 0;
    }

    static int rank(BlockState s) {
        if (s.isAir()) return 0;
        Block b = s.getBlock();
        if (b instanceof com.yearofice.worldgen.block.BrushBlock) return 1;
        if (b instanceof com.yearofice.worldgen.block.YoiLeavesBlock) return 2;
        if (b instanceof com.yearofice.worldgen.block.StumpBlock || b instanceof com.yearofice.worldgen.block.FallenLogBlock) return 3;
        if (b instanceof com.yearofice.worldgen.block.YoiLogBlock || b instanceof com.yearofice.worldgen.block.ShrubStemBlock) return 4;
        return 99;
    }

    /** A tree's base: one of our ground surfaces (not rock, not a riffle, not a placed piece). */
    static boolean stands(BlockState s) {
        Block b = s.getBlock();
        return b == WorldBlocks.FOREST_DUFF.get() || b == WorldBlocks.MOSS_MAT.get() || b == WorldBlocks.GRASS_TURF.get()
                || b == WorldBlocks.TUNDRA_TURF.get() || b == WorldBlocks.LICHEN_MAT.get() || b == WorldBlocks.SILT.get()
                || b == WorldBlocks.LOESS.get() || b == WorldBlocks.TILL.get() || b == WorldBlocks.GRAVEL.get()
                || b == WorldBlocks.RIVER_SAND.get() || b == WorldBlocks.PEAT.get() || b == WorldBlocks.CLAY_BANK.get();
    }
}
