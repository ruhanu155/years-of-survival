package com.yearofice.ecosystem.raid;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@code yearofice:theft_cache}: what a fox dragged off the rack and hid under moss or snow (Structures and
 * buildables › Thieves at the rack). Tracks lead to it. Right mouse takes the stolen item back; after 2 game days the
 * thief has eaten it and the cache is empty.
 */
public class TheftCacheBlock extends BaseEntityBlock {
    public static final MapCodec<TheftCacheBlock> CODEC = simpleCodec(TheftCacheBlock::new);

    public TheftCacheBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TheftCacheBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TheftCacheBlockEntity be) {
            var stack = be.take();
            if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
            level.removeBlock(pos, false);
        }
        return InteractionResult.CONSUME;
    }
}
