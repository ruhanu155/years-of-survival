package com.yearofice.fire;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code yearofice:flame}: fire that has jumped from a packed hearth to something burnable within 2 m (Minecraft
 * engine › Fire › Spread). Vanilla fire spread stays off; this is our own block. It replaces the piece it burns, lights
 * the place at level 15, and is gone after {@code flame_burn_min} game minutes, taking the piece with it. It may light
 * a burnable neighbour on the way (see {@link FlameBlockEntity}).
 */
public class FlameBlock extends BaseEntityBlock {
    public static final MapCodec<FlameBlock> CODEC = simpleCodec(FlameBlock::new);

    public FlameBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FlameBlockEntity(pos, state); }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
}
