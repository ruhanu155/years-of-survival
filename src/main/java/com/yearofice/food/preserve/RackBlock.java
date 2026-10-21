package com.yearofice.food.preserve;

import com.mojang.serialization.MapCodec;
import com.yearofice.food.cooking.RingHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

/**
 * {@code yearofice:drying_rack} and {@code yearofice:smoke_rack}: right mouse with food puts it on (as many items as
 * fit), an empty hand takes the load off. The smoke rack stands only over a fire ring.
 */
public class RackBlock extends BaseEntityBlock {
    public static final MapCodec<RackBlock> CODEC = simpleCodec(p -> new RackBlock(p, false));
    private final boolean smoke;

    public RackBlock(Properties props, boolean smoke) {
        super(props);
        this.smoke = smoke;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !smoke || RingHeat.isRing(level.getBlockState(pos.below()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return smoke ? new SmokeRackBlockEntity(pos, state) : new DryingRackBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RackBlockEntity be)) return ItemInteractionResult.FAIL;
        return be.putOn(stack) > 0 ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RackBlockEntity be) || be.empty()) return InteractionResult.PASS;
        for (ItemStack s : be.takeAll()) if (!player.getInventory().add(s)) player.drop(s, false);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(); }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StoreBlockEntity be) be.dropAll();
        super.onRemove(state, level, pos, newState, moved);
    }
}
