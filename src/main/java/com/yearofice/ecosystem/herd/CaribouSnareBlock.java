package com.yearofice.ecosystem.herd;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** {@code yearofice:caribou_snare}: six-strand babiche hung in the lead fence's mouth. The one exception to "never trapped". */
public class CaribouSnareBlock extends BaseEntityBlock {
    public static final MapCodec<CaribouSnareBlock> CODEC = simpleCodec(CaribouSnareBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public CaribouSnareBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CaribouSnareBlockEntity(pos, state); }
}
