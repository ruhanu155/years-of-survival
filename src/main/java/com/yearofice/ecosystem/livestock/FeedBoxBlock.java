package com.yearofice.ecosystem.livestock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * {@code yearofice:feed_trough}, {@code yearofice:water_trough}, {@code yearofice:hay_rack} (feed) and {@code yearofice:nest_box} (a hen's
 * eggs collect in it). Hand feed or a water bucket fills them; animals within 3 m draw from them each third hour
 * ({@link FeedBoxBlockEntity#draw}), so a pen can be fed for days without standing in it.
 */
public class FeedBoxBlock extends BaseEntityBlock {
    public enum Kind { FEED_TROUGH, WATER_TROUGH, HAY_RACK, NEST_BOX }

    public final Kind kind;
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(1, 0, 1, 15, 10, 15);

    public FeedBoxBlock(Kind kind, Properties p) {
        super(p);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new FeedBoxBlock(Kind.FEED_TROUGH, p)); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FeedBoxBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FeedBoxBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if ((kind == Kind.FEED_TROUGH || kind == Kind.HAY_RACK) && LivestockSystem.isFeed(stack) && be.addFeed(0.5)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.CONSUME;
        }
        if (kind == Kind.FEED_TROUGH && stack.is(Items.WATER_BUCKET) && be.addWater(10)) {
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (kind == Kind.NEST_BOX && level.getBlockEntity(pos) instanceof FeedBoxBlockEntity be) {
            for (ItemStack s : be.takeEggs()) player.getInventory().placeItemBackInInventory(s);
        }
        return InteractionResult.CONSUME;
    }
}
