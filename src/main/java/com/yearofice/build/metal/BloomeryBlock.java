package com.yearofice.build.metal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@code yearofice:bloomery} (Tier 4 › Bloomery furnace †): the controller of the clay furnace. It works when two or more
 * of its four sides are stone walls and no wood piece stands within 3 blocks. Use with ore or charcoal loads it, an empty
 * hand takes out a finished bloom, and holding use smelts ({@link SmeltJob}).
 */
public class BloomeryBlock extends BaseEntityBlock {
    public static final MapCodec<BloomeryBlock> CODEC = simpleCodec(BloomeryBlock::new);

    public BloomeryBlock(Properties p) { super(p); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BloomeryBlockEntity(pos, state); }

    /** The furnace stands: at least two walls of stone around the controller, and no wood within 3 blocks. */
    public static boolean built(Level level, BlockPos pos) {
        int walls = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) if (level.getBlockState(pos.relative(d)).is(MetalSystem.FURNACE_WALLS)) walls++;
        if (walls < 2) return false;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(q);
            if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS)) return false;
        }
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity be && BloomeryBlockEntity.oreValue(stack) + (stack.is(MetalRegistry.CHARCOAL.get()) ? 1 : 0) > 0) {
            if (!level.isClientSide) {
                int n = be.load(stack);
                player.displayClientMessage(Component.translatable("block.yearofice.bloomery.loaded", n, be.quarters() / 40, be.charcoal()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity be && be.blooms() > 0) {
            int n = be.takeBlooms();
            ItemStack out = new ItemStack(MetalRegistry.IRON_BLOOM.get(), n);
            if (!player.getInventory().add(out)) player.drop(out, false);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
