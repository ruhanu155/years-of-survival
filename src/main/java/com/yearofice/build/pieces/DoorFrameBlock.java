package com.yearofice.build.pieces;

import com.yearofice.build.rooms.Rooms;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A door frame piece: a wall course with a door in it. Closed it is a wall of high U; opened (use) it is a leak. */
public class DoorFrameBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public DoorFrameBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(OPEN); }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
        if (!l.isClientSide) {
            BlockState next = s.cycle(OPEN);
            l.setBlock(p, next, 10);
            l.playSound(null, p, next.getValue(OPEN) ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            Rooms.dirty(l, p);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
