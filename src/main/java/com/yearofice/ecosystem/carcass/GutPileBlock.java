package com.yearofice.ecosystem.carcass;

import com.yearofice.api.env.EnvService;
import com.yearofice.sim.ecosystem.GutPile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;

/**
 * {@code yearofice:gut_pile} (Decay and time › Carcasses and gut piles): what field dressing leaves on the ground. It
 * is eaten and rots in four stages, 3 game days in summer and about 30 in winter ({@link GutPile}); it makes no
 * item, and a carcass or gut pile is a bear bait station (Setting up the hunt), which the predator systems read from
 * the block. Walk-through, flat.
 */
public class GutPileBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    /** Ticks between rolls: one game hour. */
    static final int HOUR_TICKS = (int) Math.round(com.yearofice.ecosystem.clock.SliceClock.TICKS_PER_MINUTE * 60);

    public GutPileBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return net.minecraft.world.phys.shapes.Shapes.empty(); }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, HOUR_TICKS);
            com.yearofice.ecosystem.ai.ScentPoints.add((ServerLevel) level, pos);
        }
    }

    /** Each game hour the pile advances one of four stages with the chance that makes its life the temperature's. */
    @Override
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        double airC = EnvService.installed() ? EnvService.get().airC(level, pos) : 5;
        double p = 4.0 / (GutPile.lifeDays(airC) * 24.0);
        if (r.nextDouble() < p) {
            int age = s.getValue(AGE);
            if (age >= 3) {
                com.yearofice.ecosystem.ai.ScentPoints.remove(level, pos);
                level.removeBlock(pos, false);
                return;
            }
            level.setBlock(pos, s.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, HOUR_TICKS);
    }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState n, net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockPos np) {
        return d == Direction.DOWN && !canSurvive(s, l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
    }
}
