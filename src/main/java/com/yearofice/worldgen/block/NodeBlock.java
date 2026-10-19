package com.yearofice.worldgen.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Resource nodes in the rock shell, placed only where rock touches air or water (Explore, don't dig): taking from
 * one (tools, M3) counts its property down.
 */
public final class NodeBlock {
    private NodeBlock() {}

    /** {@code iron_outcrop}, {@code nodules} 0–8. */
    public static class Iron extends Block {
        public static final IntegerProperty NODULES = IntegerProperty.create("nodules", 0, 8);

        public Iron(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(NODULES, 8));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NODULES); }
    }

    /** {@code coal_seam}, {@code remaining} 0–6 and {@code burning} (a seam fire, M7). */
    public static class Coal extends Block {
        public static final IntegerProperty REMAINING = IntegerProperty.create("remaining", 0, 6);
        public static final BooleanProperty BURNING = BooleanProperty.create("burning");

        public Coal(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(REMAINING, 6).setValue(BURNING, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(REMAINING, BURNING); }
    }
}
