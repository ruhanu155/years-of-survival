package com.yearofice.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code snow_cover} (Snow cover and permafrost blocks): {@code layers} 1–8 of 12.5 cm, {@code crust},
 * {@code wet}. Powder collides as one layer, so the walker sinks in; a crust collides at full height. No random
 * ticks: the snow pass sets layers from the Environment field (M7). Roof snow is this block too.
 */
public class SnowCoverBlock extends Block {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    public static final BooleanProperty CRUST = BooleanProperty.create("crust");
    public static final BooleanProperty WET = BooleanProperty.create("wet");
    /** Trail wear 0 none, 1 broken, 2 set (hard overnight), 3 sled lane (travel lane D owns writes). */
    public static final IntegerProperty TRAIL_WEAR = IntegerProperty.create("trail_wear", 0, 3);
    static final VoxelShape[] BY_LAYER = new VoxelShape[9];

    static {
        for (int i = 0; i <= 8; i++) BY_LAYER[i] = Block.box(0, 0, 0, 16, i * 2, 16);
    }

    public SnowCoverBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1).setValue(CRUST, false).setValue(WET, false).setValue(TRAIL_WEAR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LAYERS, CRUST, WET, TRAIL_WEAR); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return BY_LAYER[s.getValue(LAYERS)]; }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return BY_LAYER[s.getValue(CRUST) ? s.getValue(LAYERS) : 1];
    }
}
