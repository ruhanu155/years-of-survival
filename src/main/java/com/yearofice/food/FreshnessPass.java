package com.yearofice.food;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.EatingLadder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The slice's stand-in for the body's hourly rehoming pass (Layer 5, Rehoming): every 5 s a player's food is
 * settled. A food of ours that came from somewhere without a stamp (a command, the creative tab) is stamped new and
 * raw; a stack that has rotted is removed ("Rotten: Removed", Blueprint › Spoilage model). Nothing else ticks.
 */
public final class FreshnessPass {
    private FreshnessPass() {}

    static final int EVERY_TICKS = 100;

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % EVERY_TICKS != 0) return;
        settle(p);
    }

    /** Stamps unstamped food and removes rotten stacks; returns how many stacks rotted away. */
    public static int settle(ServerPlayer p) {
        Inventory inv = p.getInventory();
        DecayService decay = DecayService.get();
        int rotted = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !ours(s) || FoodMap.row(s) == null) continue;
            Fresh f = decay.settle(p.level(), s);
            if (f == null) {
                decay.stamp(p.level(), s, FoodState.RAW);
                continue;
            }
            if (decay.stage(p.level(), s) >= EatingLadder.ROTTEN) {
                p.displayClientMessage(Component.translatable("food.yearofice.rotted_away", s.getHoverName()), false);
                inv.setItem(i, ItemStack.EMPTY);
                rotted++;
            }
        }
        return rotted;
    }

    static boolean ours(ItemStack s) {
        return YearOfIce.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())
                && (s.getItem() instanceof YoiFoodItem || s.has(YoiComponents.FRESH.get()));
    }
}
