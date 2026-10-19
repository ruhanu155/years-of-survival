package com.yearofice.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** {@code shrub_stem}: a willow or alder stem, a 4 px pole. One shrub record per stool joins with the plants (M5). */
public class ShrubStemBlock extends Block {
    static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public ShrubStemBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
}
