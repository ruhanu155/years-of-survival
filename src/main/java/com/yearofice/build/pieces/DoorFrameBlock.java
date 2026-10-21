package com.yearofice.build.pieces;

import com.yearofice.build.rooms.Rooms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A door frame piece: a wall course with a door in it. Closed it is a wall of high U; opened (use) it is a leak. */
public class DoorFrameBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public DoorFrameBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(OPEN); }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
        if (!l.isClientSide) {
            l.setBlock(p, s.cycle(OPEN), 10);
            Rooms.dirty(l, p);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
