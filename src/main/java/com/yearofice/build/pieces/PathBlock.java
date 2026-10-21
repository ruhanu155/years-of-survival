package com.yearofice.build.pieces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A path laid on the ground; its {@code surface} sets the walking speed bonus ({@link PathSpeed}). */
public class PathBlock extends Block {
    public enum Surface implements StringRepresentable {
        DIRT, GRAVEL, PLANK, STONE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final EnumProperty<Surface> SURFACE = EnumProperty.create("surface", Surface.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public PathBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(SURFACE, Surface.DIRT));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(SURFACE); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP); }
}
