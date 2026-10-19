package com.yearofice.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code riffle_cobble}: the stones of a riffle lip, 6 px high. The column builder sets a row of them where a
 * stream's surface steps down one block, on gravel, so the upper water never meets air at its own level and
 * never runs (Rivers, current and water; {@code worldgen/no_spill}). Not waterloggable, so water cannot enter it.
 */
public class RiffleCobbleBlock extends Block {
    static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public RiffleCobbleBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
}
