package com.yearofice.build.store;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
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

/** Crate, barrel, shelf, hanging bar and cache-leg platform: use food on it to store, an empty hand to take it all. */
public class StoreBlock extends BaseEntityBlock {
    public static final MapCodec<StoreBlock> CODEC = simpleCodec(StoreBlock::new);

    public StoreBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StoreContainerBlockEntity(pos, state); }


    /** A raised cache platform more than 2.5 m above the player's feet is out of reach: the Messenger says a ladder is needed. */
    private static boolean tooHigh(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof StoreContainerBlockEntity be) || be.heightM() < 2 || pos.getY() - player.getY() <= 2.5) return false;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.yearofice.api.hud.HudBus.get().post(sp,
                com.yearofice.api.hud.HudLine.of(com.yearofice.api.hud.HudLineType.WARN, "cache_needs_ladder", "build.store"));
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (tooHigh(level, pos, player)) return ItemInteractionResult.CONSUME;
        if (level.getBlockEntity(pos) instanceof StoreContainerBlockEntity be && be.putOn(stack) > 0) return ItemInteractionResult.CONSUME;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (tooHigh(level, pos, player)) return InteractionResult.CONSUME;
        if (level.getBlockEntity(pos) instanceof StoreContainerBlockEntity be) {
            for (ItemStack s : be.takeAll()) player.getInventory().placeItemBackInInventory(s);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StoreContainerBlockEntity be) be.dropAll();
        super.onRemove(state, level, pos, newState, moved);
    }
}
