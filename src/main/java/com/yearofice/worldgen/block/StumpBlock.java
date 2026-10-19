package com.yearofice.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** {@code stump}: what a fallen or felled tree leaves, 10 px high; {@code age} 0–3 rots on Decay's day pass (M5). */
public class StumpBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public StumpBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
}
