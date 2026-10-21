package com.yearofice.env.weather;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** {@code fallen_limb}: a limb a glaze or a strike brought down. Loot is half an armload of wood. */
public class FallenLimbBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FallenLimbBlock> CODEC = simpleCodec(FallenLimbBlock::new);
    private static final VoxelShape X = Block.box(0, 0, 5, 16, 4, 11), Z = Block.box(5, 0, 0, 11, 4, 16);

    public FallenLimbBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(FACING).getAxis() == Direction.Axis.X ? Z : X;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection()); }
}
