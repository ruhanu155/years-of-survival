package com.yearofice.env.weather;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code glaze_layer} (one layer per 5 mm of glaze) and {@code hail_layer} (1–2 layers): a thin sheet on a top face, set and
 * removed by {@link GlazePass}. Slipperiness is the block's friction, so the bare-ice slip rule reads it.
 */
public class SheetBlock extends Block {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 3);
    public static final MapCodec<SheetBlock> CODEC = simpleCodec(SheetBlock::new);

    public SheetBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LAYERS); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Block.box(0, 0, 0, 16, s.getValue(LAYERS), 16);
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
        return d == Direction.DOWN && !canSurvive(s, l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState(); }
}
