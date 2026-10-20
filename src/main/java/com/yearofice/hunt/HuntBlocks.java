package com.yearofice.hunt;

import com.yearofice.api.clock.ClockService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The ground blind and the tree stand (Blueprint › Setting up the hunt › Build sheet). Their effect on senses is {@link HuntConcealment}. */
public final class HuntBlocks {
    private HuntBlocks() {}

    /** A screen of boughs. It is {@code fresh} for the first game day, when it only halves the sight of moose, caribou and sheep. */
    public static final class PoleBlind extends Block {
        public static final BooleanProperty FRESH = BooleanProperty.create("fresh");
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 24, 16);

        public PoleBlind(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FRESH, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FRESH); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

        @Override
        protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0, 0, 0, 16, 16, 16); }

        @Override
        protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
            if (l instanceof ServerLevel sl && s.getValue(FRESH) && !old.is(this)) {
                int ds = ClockService.installed() ? Math.max(1, ClockService.get().dsPerTick()) : 1;
                sl.scheduleTick(p, this, (int) Math.min(Integer.MAX_VALUE / 2, 1440L * 600L / ds));
            }
        }

        @Override
        protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { l.setBlock(p, s.setValue(FRESH, false), 2); }
    }

    /** Poles lashed in a spruce, a platform at 3 m. */
    public static final class TreeStand extends Block {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

        public TreeStand(Properties p) { super(p); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    }
}
