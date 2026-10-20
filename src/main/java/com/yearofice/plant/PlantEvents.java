package com.yearofice.plant;

import com.yearofice.api.event.ClockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Right mouse on a plant (harvest verb by tool; pocket guide opens the card, crouched it tastes), eating a part, and the day pass. */
public final class PlantEvents {
    private PlantEvents() {}

    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        BlockPos pos = e.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PlantBlock)) return;
        ItemStack tool = p.getMainHandItem();
        if (tool.is(com.yearofice.item.inventory.KitItems.PLANT_GUIDE.get())) {
            Inspect.say(p, p.isShiftKeyDown() ? Inspect.taste(p, level, pos) : Inspect.card(p, level, pos));
        } else {
            Inspect.say(p, HarvestVerbs.act(p, level, pos, HarvestVerbs.verbFor(tool, p.isShiftKeyDown())).message());
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Eating a plant's part identifies the plant (the blueprint's taste-by-meal route). */
    public static void onEaten(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlantSpec spec = PlantData.ofPart(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString());
        if (spec != null) Knowledge.identify(p, spec.id);
    }

    public static void onDay(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() instanceof ServerLevel level) PlantPhasePass.around(level, p.blockPosition(), 24);
        }
    }
}
