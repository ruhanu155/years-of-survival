package com.yearofice.crop;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.plant.Inspect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Harvest on a ripe crop, the crouch card on a bed, and the daily growth pass over crops near players. */
public final class CropEvents {
    private CropEvents() {}

    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        BlockPos pos = e.getPos();
        var block = level.getBlockState(pos).getBlock();
        if (block instanceof CropBlock) {
            if (level.getBlockEntity(pos) instanceof CropBlockEntity be) Garden.catchUp(level, pos, be);
            ItemStack got = Garden.harvest(level, pos, p);
            if (got.is(CropRegistry.POTATO.get())) com.yearofice.story.ambition.Ambitions.fire(p, "crop.potatoes_dug", level.getGameTime() / 1200);
            if (got.is(CropRegistry.WHEAT.get()) || got.is(CropRegistry.BARLEY.get()) || got.is(CropRegistry.GRAIN_SHEAF.get())) {
                com.yearofice.api.story.AmbitionTracker.tally(p, "crop.grew_grain", 1);
            }
            if (got.has(CropRegistry.SEED_LOT.get())) {
                com.yearofice.story.ambition.Ambitions.fire(p, "crop.seed_saved", level.getGameTime() / 1200);
            }
            Inspect.say(p, got.isEmpty() ? Garden.card(level, pos.below(), null) : "Harvested " + got.getHoverName().getString() + ".");
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        } else if (block instanceof GardenBedBlock && p.isShiftKeyDown()) {
            ItemStack held = p.getMainHandItem();
            CropSpec seed = held.isEmpty() ? null : CropData.bySeed(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            Inspect.say(p, Garden.card(level, pos, seed));
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void onDay(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!(p.level() instanceof ServerLevel level)) continue;
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 6, 24))) {
                if (level.isLoaded(q) && level.getBlockState(q).getBlock() instanceof CropBlock && level.getBlockEntity(q) instanceof CropBlockEntity be) {
                    Garden.catchUp(level, q.immutable(), be);
                }
            }
        }
    }

    /** Right mouse in the air or on a block with a hand quern or a flail in the main hand: grind or thresh the off-hand stack. */
    public static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel)) return;
        String msg = Grain.use(p);
        if (msg == null) return;
        Inspect.say(p, msg);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
