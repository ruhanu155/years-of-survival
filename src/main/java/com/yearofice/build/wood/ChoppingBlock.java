package com.yearofice.build.wood;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code yearofice:chopping_block} (Trees, felling and firewood › Splitting): a round set on end, or a stump
 * ({@code stump=true}, which keeps the stump's look). Use with a round sets it on the block; use with an empty hand
 * takes it back; holding use with an axe splits it ({@link WoodJobs.Split}). The round lives in the block entity.
 */
public class ChoppingBlock extends BaseEntityBlock {
    public static final MapCodec<ChoppingBlock> CODEC = simpleCodec(ChoppingBlock::new);
    public static final BooleanProperty STUMP = BooleanProperty.create("stump");
    static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public ChoppingBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(STUMP, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(STUMP); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ChoppingBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChoppingBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (WoodItems.isRound(held) && !be.hasRound() && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                be.setRound(held, level.random.nextInt(4));
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && be.hasRound()) {
            if (!level.isClientSide) WoodItems.give(player, be.takeRound());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moved) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof ChoppingBlockEntity be && be.hasRound()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, be.takeRound());
        }
        super.onRemove(state, level, pos, now, moved);
    }
}
