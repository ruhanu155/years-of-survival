package com.yearofice.build.store;

import com.mojang.serialization.MapCodec;
import com.yearofice.build.wood.WoodItems;
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

/** Racks under a roof for split wood: seasoning at the woodshed rate. */
public class FirewoodShedBlock extends BaseEntityBlock {
    public static final MapCodec<FirewoodShedBlock> CODEC = simpleCodec(FirewoodShedBlock::new);

    public FirewoodShedBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FirewoodShedBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        int before = stack.getCount();
        if (level.getBlockEntity(pos) instanceof FirewoodShedBlockEntity be && be.put(stack, WoodItems.today())) {
            int added = Math.max(1, before - stack.getCount());
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                if (com.yearofice.api.story.AmbitionTracker.tally(sp, "wood.seasoned_splits", added) >= 300) {
                    com.yearofice.story.ambition.Ambitions.fire(sp, "wood.seasoned_300",
                            com.yearofice.api.clock.ClockService.installed() ? com.yearofice.api.clock.ClockService.get().nowMinute() : 0);
                }
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof FirewoodShedBlockEntity be) for (ItemStack s : be.takeAll(WoodItems.today())) player.getInventory().placeItemBackInInventory(s);
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) { return List.of(new ItemStack(this)); }
}
