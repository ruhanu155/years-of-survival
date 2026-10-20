package com.yearofice.hunt.fishing;

import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.fishing.FishCut;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The knife cuts a landed fish into chunks: right mouse with a knife in one hand and a whole fish in the other turns
 * the fish into {@code yearofice:fish_chunk} bait, one chunk a 150 g and at least two ({@link FishCut}).
 */
public final class FishCutting {
    private FishCutting() {}

    static boolean isKnife(ItemStack s) {
        if (s.isEmpty()) return false;
        String p = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return p.equals("knife") || p.equals("fixed_blade_knife") || p.equals("flint_knife");
    }

    static boolean isFish(ItemStack s) {
        return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals("yearofice")
                && FishingSpecies.IDS.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath());
    }

    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isKnife(p.getMainHandItem()) || !isFish(p.getOffhandItem())) return;
        int chunks = cut(p, p.getOffhandItem());
        if (chunks > 0) {
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
        }
    }

    /** Cuts the fish stack: it is consumed and its chunks go to the player's hands or the ground. Returns the chunks. */
    public static int cut(ServerPlayer p, ItemStack fish) {
        int grams = fish.getOrDefault(YoiComponents.WEIGHT_G.get(), 600);
        int n = FishCut.chunks(grams);
        fish.shrink(1);
        ItemStack out = new ItemStack(FishingRegistry.FISH_CHUNK.get(), n);
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Cut the fish into " + n + " chunks."), true);
        return n;
    }
}
