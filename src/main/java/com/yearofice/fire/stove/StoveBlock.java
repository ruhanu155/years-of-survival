package com.yearofice.fire.stove;

import com.mojang.serialization.MapCodec;
import com.yearofice.build.BuildRegistry;
import com.yearofice.fire.FireParts;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FuelData;
import com.yearofice.sim.env.FireSetting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A tin, barrel or masonry stove. Use split wood to load it, a match to light it, spruce boughs to sweep the
 * chimney; an empty hand cycles Low, Steady, Hot. Which kind it is comes from its registry id and the
 * {@code multiblock/<id>.json} row.
 */
public class StoveBlock extends BaseEntityBlock {
    public static final MapCodec<StoveBlock> CODEC = simpleCodec(StoveBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public StoveBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StoveBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, StoveRegistry.STOVE_ENTITY.get(), StoveBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof StoveBlockEntity be)) return ItemInteractionResult.FAIL;
        FuelData fuel = FireParts.fuel(stack);
        if (fuel != null) {
            double take = be.loadFuel(fuel.woodType(), FireParts.moisture(stack), fuel.armloadFraction());
            if (take > 0 && !player.getAbilities().instabuild) stack.shrink(1);
            return take > 0 ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }
        if (stack.is(FireRegistry.MATCH.get()) || stack.is(FireRegistry.FIRE_STARTER.get())) {
            boolean lit = be.light();
            if (lit && stack.is(FireRegistry.MATCH.get()) && !player.getAbilities().instabuild) stack.shrink(1);
            return lit ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }
        if (stack.is(BuildRegistry.SPRUCE_BOUGHS.get()) && stack.getCount() >= StoveBlockEntity.SWEEP_BOUGHS) {
            if (!player.getAbilities().instabuild) stack.shrink(StoveBlockEntity.SWEEP_BOUGHS);
            be.sweep();
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof StoveBlockEntity be) {
            FireSetting[] all = FireSetting.values();
            be.setting(all[(be.setting().ordinal() + 1) % all.length]);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(new ItemStack(this)); }
}
