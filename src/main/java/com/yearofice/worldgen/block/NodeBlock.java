package com.yearofice.worldgen.block;

import com.yearofice.api.clock.ClockService;
import com.yearofice.build.metal.MetalRegistry;
import com.yearofice.story.ambition.Ambitions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Resource nodes in the rock shell, placed only where rock touches air or water (Explore, don't dig): taking from
 * one (tools, M3) counts its property down.
 */
public final class NodeBlock {
    private NodeBlock() {}

    /** {@code iron_outcrop}, {@code nodules} 0–8. */
    public static class Iron extends Block {
        public static final IntegerProperty NODULES = IntegerProperty.create("nodules", 0, 8);

        public Iron(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(NODULES, 8));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NODULES); }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
            int n = s.getValue(NODULES);
            if (n <= 0) return InteractionResult.PASS;
            if (!l.isClientSide) {
                popResource(l, p.relative(h.getDirection()), new ItemStack(MetalRegistry.IRON_NODULE.get()));
                int next = n - 1;
                l.setBlock(p, next > 0 ? s.setValue(NODULES, next) : WorldBlocks.FACE_ROCK.get().defaultBlockState(), 3);
                l.playSound(null, p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.9f, 0.85f);
                if (pl instanceof ServerPlayer sp) {
                    Ambitions.fire(sp, "ore.surface_iron", ClockService.installed() ? ClockService.get().nowMinute() : 0);
                }
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }

        @Override
        public BlockState playerWillDestroy(Level l, BlockPos p, BlockState s, Player pl) {
            if (!l.isClientSide && !pl.isCreative() && s.getValue(NODULES) > 0) {
                popResource(l, p, new ItemStack(MetalRegistry.IRON_NODULE.get()));
                if (pl instanceof ServerPlayer sp) {
                    Ambitions.fire(sp, "ore.surface_iron", ClockService.installed() ? ClockService.get().nowMinute() : 0);
                }
            }
            return super.playerWillDestroy(l, p, s, pl);
        }
    }

    /** {@code coal_seam}, {@code remaining} 0–6 and {@code burning} (a seam fire, M7). */
    public static class Coal extends Block {
        public static final IntegerProperty REMAINING = IntegerProperty.create("remaining", 0, 6);
        public static final BooleanProperty BURNING = BooleanProperty.create("burning");

        public Coal(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(REMAINING, 6).setValue(BURNING, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(REMAINING, BURNING); }
    }
}
