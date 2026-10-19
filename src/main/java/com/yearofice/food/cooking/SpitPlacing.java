package com.yearofice.food.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right mouse with roastable food on a fire ring sets a spit over it and puts the food on (Blueprint › Cooking: the
 * spit or stick roast). The ring's own interactions (parts, matches, settings) are untouched: only an item that
 * {@code cooking/foods.json} lists is caught here, before the ring sees it. Server decides; the client just swings.
 */
public final class SpitPlacing {
    private SpitPlacing() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        if (level.isClientSide) return;
        if (tryPlace(level, e.getPos(), e.getEntity(), e.getHand())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** True if the held food went onto a spit over the ring at {@code pos} (setting the spit up if needed). */
    public static boolean tryPlace(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || CookingData.of(held.getItem()) == null || !RingHeat.isRing(level.getBlockState(pos))) return false;
        BlockPos above = pos.above();
        BlockState there = level.getBlockState(above);
        boolean placed = false;
        if (!there.is(CookingRegistry.SPIT.get())) {
            if (!there.isAir()) return false;
            level.setBlockAndUpdate(above, CookingRegistry.SPIT.get().defaultBlockState());
            placed = true;
        }
        if (!(level.getBlockEntity(above) instanceof SpitBlockEntity be) || !be.empty()) return false;
        int n = be.putOn(held);
        if (n == 0) {
            if (placed) level.removeBlock(above, false);
            return false;
        }
        level.playSound(null, above, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5f, 1.2f);
        return true;
    }
}
