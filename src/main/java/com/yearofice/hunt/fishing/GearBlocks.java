package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.PassiveGear;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The passive gear placed by hand (Blueprint › Fishing in depth › Passive gear; Waters and fish › Every method in
 * keys). The blocks hold no state: each piece is a record in {@link PassiveGearService} keyed by position, so the
 * dawn and dusk settling needs no loaded chunk. Right mouse checks (and empties) a piece; right mouse with bait
 * baits a set line. The debug commands in {@code /yoi fish gear} place the same records without blocks.
 */
public final class GearBlocks {
    private GearBlocks() {}

    static long now(ServerLevel level) { return FishingClock.minute(level.getServer()); }

    /** Settles and empties the gear at {@code pos} into the player's hands, with a line saying what was in it. */
    static InteractionResult checkAt(Level level, BlockPos pos, Player player, PassiveGearService.Kind kind) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        PassiveGearService.Gear g = kind == PassiveGearService.Kind.GILL_NET
                ? PassiveGearService.nearest(sl, pos, PassiveGearService.Kind.GILL_NET, NET_REACH) : PassiveGearService.at(sl, pos);
        if (g == null) {
            sp.displayClientMessage(Component.literal("Nothing is set here."), true);
            return InteractionResult.CONSUME;
        }
        if (g.kind == PassiveGearService.Kind.GILL_NET && g.segments < PassiveGear.NET_MIN_SEGMENTS) {
            sp.displayClientMessage(Component.literal("Net: " + g.segments + " segment(s). Under 4 joined it will not fish."), true);
        }
        PassiveGearService.Settled s = PassiveGearService.settle(sl, g, now(sl));
        List<ItemStack> out = PassiveGearService.check(sl, sp, g, now(sl));
        String what = switch (g.kind) {
            case SET_LINE -> "Set line";
            case GILL_NET -> "Net (" + g.segments + " segments)";
            case FISH_TRAP -> "Fish trap";
        };
        sp.displayClientMessage(Component.literal(out.isEmpty() ? what + ": empty." : what + ": " + out.size() + " fish."
                + (s.stolen() + s.eaten() > 0 ? " Some were taken." : "")), true);
        return InteractionResult.CONSUME;
    }

    static final int NET_REACH = 6;

    /** A stake on the bank with up to five baited hooks on a line. */
    public static class SetLine extends Block {
        public static final int HOOKS = 5;
        private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 12, 11);

        public SetLine(Properties p) { super(p); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            if (level instanceof ServerLevel sl && !old.is(this) && PassiveGearService.at(sl, pos) == null)
                PassiveGearService.place(sl, PassiveGearService.Kind.SET_LINE, pos, HOOKS, now(sl));
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nw, boolean moved) {
            if (level instanceof ServerLevel sl && !state.is(nw.getBlock())) PassiveGearService.remove(sl, pos);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                  InteractionHand hand, BlockHitResult hit) {
            var key = FishBaits.key(stack);
            if (key.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level instanceof ServerLevel sl) {
                PassiveGearService.Gear g = PassiveGearService.at(sl, pos);
                if (g != null) {
                    // Hooks fish from the moment they are baited, never before.
                    PassiveGearService.settle(sl, g, now(sl));
                    g.bait = key.get();
                    PassiveGearService.book(sl).setDirty();
                    if (!FishBaits.isLure(g.bait) && !player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal("Hooks baited with " + g.bait.replace('_', ' ') + "."), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return checkAt(level, pos, player, PassiveGearService.Kind.SET_LINE);
        }
    }

    /** One metre of net, 2 tall, usually waterlogged. A segment item places three in a line. */
    public static class GillNet extends Block implements SimpleWaterloggedBlock {
        public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

        public GillNet(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(WATERLOGGED); }

        @Override
        protected FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(7, 0, 0, 9, 16, 16); }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nw, boolean moved) {
            if (level instanceof ServerLevel sl && !state.is(nw.getBlock())) {
                PassiveGearService.Gear g = PassiveGearService.at(sl, pos);
                if (g != null && g.kind == PassiveGearService.Kind.GILL_NET) PassiveGearService.remove(sl, pos);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return checkAt(level, pos, player, PassiveGearService.Kind.GILL_NET);
        }
    }

    /** The basket in a fence's gap: it fishes on its own and is emptied by hand. */
    public static class WeirBasket extends Block {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

        public WeirBasket(Properties p) { super(p); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            if (level instanceof ServerLevel sl && !old.is(this) && PassiveGearService.at(sl, pos) == null)
                PassiveGearService.place(sl, PassiveGearService.Kind.FISH_TRAP, pos, 0, now(sl));
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nw, boolean moved) {
            if (level instanceof ServerLevel sl && !state.is(nw.getBlock())) PassiveGearService.remove(sl, pos);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return checkAt(level, pos, player, PassiveGearService.Kind.FISH_TRAP);
        }
    }

    /**
     * A net segment: RMB on the bank places three net blocks (2 tall) in the direction the player faces, and adds a
     * segment to the net within {@link #NET_REACH} blocks, or starts a net. Four joined segments are the minimum
     * that fishes.
     */
    public static class NetSegmentItem extends Item {
        public NetSegmentItem(Properties p) { super(p); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            Player player = ctx.getPlayer();
            BlockPos base = level.getBlockState(ctx.getClickedPos()).canBeReplaced() ? ctx.getClickedPos() : ctx.getClickedPos().relative(ctx.getClickedFace());
            Direction dir = player == null ? ctx.getHorizontalDirection() : player.getDirection();
            int placed = 0;
            for (int i = 0; i < 3; i++) {
                for (int y = 0; y < 2; y++) {
                    BlockPos at = base.relative(dir, i).above(y);
                    BlockState cur = level.getBlockState(at);
                    if (!cur.canBeReplaced() && !cur.isAir()) continue;
                    boolean water = cur.getFluidState().is(Fluids.WATER) && cur.getFluidState().isSource();
                    level.setBlock(at, FishingRegistry.GILL_NET.get().defaultBlockState().setValue(GillNet.WATERLOGGED, water), 3);
                    placed++;
                }
            }
            if (placed == 0) return InteractionResult.FAIL;
            PassiveGearService.addSegment(sl, base, now(sl));
            if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            if (player instanceof ServerPlayer sp) {
                PassiveGearService.Gear g = PassiveGearService.nearest(sl, base, PassiveGearService.Kind.GILL_NET, NET_REACH);
                sp.displayClientMessage(Component.literal("Net: " + g.segments + " segment" + (g.segments == 1 ? "" : "s")
                        + (g.segments < PassiveGear.NET_MIN_SEGMENTS ? ". Join " + (PassiveGear.NET_MIN_SEGMENTS - g.segments) + " more to fish." : ".")), true);
            }
            return InteractionResult.CONSUME;
        }
    }
}
