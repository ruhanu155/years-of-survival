package com.yearofice.build.wood;

import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.worldgen.Cover;
import com.yearofice.worldgen.block.StumpBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import com.yearofice.worldgen.block.YoiLeavesBlock;
import com.yearofice.worldgen.block.YoiLogBlock;
import com.yearofice.worldgen.structure.PlacedMask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which world blocks are which tree: the log and leaf blocks of each kind, and the probe the fall search reads
 * the level through. A block is a stop when it is in {@code #yearofice:fell_stops}, is a log with {@code placed=true},
 * or has its {@code placed_mask} bit (server levels only; the client's preview has no mask and needs none, since a
 * placed log carries {@code placed=true} anyway).
 */
public final class WoodBlocks {
    private WoodBlocks() {}

    /** The tree kind of a log or leaves block, or null. */
    public static @Nullable TreeKind kindOf(Block b) {
        if (b == WorldBlocks.WHITE_SPRUCE_LOG.get() || b == WorldBlocks.WHITE_SPRUCE_LEAVES.get()) return TreeKind.WHITE_SPRUCE;
        if (b == WorldBlocks.BLACK_SPRUCE_LOG.get() || b == WorldBlocks.BLACK_SPRUCE_LEAVES.get()) return TreeKind.BLACK_SPRUCE;
        if (b == WorldBlocks.BIRCH_LOG.get() || b == WorldBlocks.BIRCH_LEAVES.get()) return TreeKind.BIRCH;
        return null;
    }

    public static Block log(TreeKind k) {
        return switch (k) {
            case BLACK_SPRUCE -> WorldBlocks.BLACK_SPRUCE_LOG.get();
            case BIRCH -> WorldBlocks.BIRCH_LOG.get();
            default -> WorldBlocks.WHITE_SPRUCE_LOG.get();
        };
    }

    public static Block leaves(TreeKind k) {
        return switch (k) {
            case BLACK_SPRUCE -> WorldBlocks.BLACK_SPRUCE_LEAVES.get();
            case BIRCH -> WorldBlocks.BIRCH_LEAVES.get();
            default -> WorldBlocks.WHITE_SPRUCE_LEAVES.get();
        };
    }

    /** The worldgen species number ({@link Cover}) for the tree shapes. */
    public static int coverSpecies(TreeKind k) {
        return switch (k) {
            case BLACK_SPRUCE -> Cover.BLACK_SPRUCE;
            case BIRCH -> Cover.BIRCH;
            default -> Cover.WHITE_SPRUCE;
        };
    }

    public static @Nullable TreeKind fromCover(int species) {
        return switch (species) {
            case Cover.WHITE_SPRUCE -> TreeKind.WHITE_SPRUCE;
            case Cover.BLACK_SPRUCE -> TreeKind.BLACK_SPRUCE;
            case Cover.BIRCH -> TreeKind.BIRCH;
            default -> null;
        };
    }

    public static boolean isLog(BlockState s) { return s.getBlock() instanceof YoiLogBlock; }

    /** A placed log: {@code placed=true}, or any log in the stop tag. */
    public static boolean placedState(BlockState s) {
        return s.getBlock() instanceof YoiLogBlock && (s.getValue(YoiLogBlock.PLACED) || s.is(WoodRegistry.FELL_STOPS));
    }

    /** Whether the block at {@code pos} is a stop for the fall search. */
    public static boolean isStop(BlockGetter level, BlockPos pos, BlockState s) {
        if (s.is(WoodRegistry.FELL_STOPS)) return true;
        if (s.getBlock() instanceof YoiLogBlock && s.getValue(YoiLogBlock.PLACED)) return true;
        return level instanceof net.minecraft.server.level.ServerLevel sl && !s.isAir() && PlacedMask.get(sl).isPlaced(pos);
    }

    /** The fall search's view of a level. */
    public static TrunkSearch.Probe probe(BlockGetter level) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        return (x, y, z) -> {
            mp.set(x, y, z);
            BlockState s = level.getBlockState(mp);
            if (s.isAir()) return TrunkSearch.Seen.OTHER;
            if (isStop(level, mp, s)) return TrunkSearch.Seen.STOP;
            Block b = s.getBlock();
            if (b instanceof YoiLogBlock) {
                TreeKind k = kindOf(b);
                return k == null ? TrunkSearch.Seen.STOP : new TrunkSearch.Seen(TrunkSearch.Cell.LOG, k.ordinal());
            }
            if (b instanceof YoiLeavesBlock) {
                TreeKind k = kindOf(b);
                return k == null ? TrunkSearch.Seen.OTHER : new TrunkSearch.Seen(TrunkSearch.Cell.LEAVES, k.ordinal());
            }
            if (b instanceof StumpBlock || b instanceof ChoppingBlock) return TrunkSearch.Seen.OTHER;
            return s.isFaceSturdy(level, mp, Direction.UP) ? TrunkSearch.Seen.GROUND : TrunkSearch.Seen.OTHER;
        };
    }
}
