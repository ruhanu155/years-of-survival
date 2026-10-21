package com.yearofice.story.radio;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@code yearofice:radio_desk}: use to tune in (a sked costs battery), sneak-use to turn the hand crank, a spare
 * {@code radio_battery} in hand tops it up.
 */
public class RadioDeskBlock extends BaseEntityBlock {
    public static final MapCodec<RadioDeskBlock> CODEC = simpleCodec(RadioDeskBlock::new);

    public RadioDeskBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RadioDeskBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!held.is(RadioRegistry.RADIO_BATTERY.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RadioDeskBlockEntity be) {
            be.charge(RadioConfig.get("battery_item_pct"));
            held.shrink(1);
            player.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "Radio battery %.0f%%.", be.battery())), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof RadioDeskBlockEntity be))
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            be.charge(RadioConfig.get("crank_pct"));
            sp.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "You turn the hand crank: %.0f%%.", be.battery())), true);
        } else if (be.drain(RadioConfig.get("sked_cost_pct"))) {
            Radio.tuneIn(sl, sp, pos);
        } else {
            sp.displayClientMessage(Component.literal("The set is dead. Crank it, or fit a battery."), true);
        }
        return InteractionResult.CONSUME;
    }
}
