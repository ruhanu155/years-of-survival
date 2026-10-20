package com.yearofice.food;

import com.yearofice.food.cooking.CookingData;
import com.yearofice.food.cooking.CookingRegistry;
import com.yearofice.food.cooking.SpitPlacing;
import com.yearofice.food.debug.FoodCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Wires cooking and eating for the slice: the spit, {@code cooking/foods.json}, the right-mouse hook on fire rings,
 * the freshness pass, {@code /yoi food} and {@code /yoi decay}, and config reloads. Called once from the mod
 * constructor. (Traps are wired by the ecosystem, which built them.)
 */
public final class FoodSystem {
    private FoodSystem() {}

    public static void init(IEventBus modBus) {
        CookingRegistry.register(modBus);
        FoodItems.register(modBus);
        com.yearofice.food.preserve.PreserveRegistry.register(modBus);
        com.yearofice.food.preserve.PreserveClock.init();
        modBus.addListener((ModConfigEvent.Loading e) -> { FoodConfig.COOKING.invalidate(); FoodConfig.RACKS.invalidate(); FoodConfig.DEADFALL.invalidate(); FoodConfig.PAN.invalidate(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { FoodConfig.COOKING.invalidate(); FoodConfig.RACKS.invalidate(); FoodConfig.DEADFALL.invalidate(); FoodConfig.PAN.invalidate(); });
        NeoForge.EVENT_BUS.addListener(CookingData::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(SpitPlacing::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(FreshnessPass::onPlayerTick);
        com.yearofice.food.cooking.SpitClock.init();
        com.yearofice.food.traps.TrapClock.init();
        NeoForge.EVENT_BUS.addListener(FoodCommands::register);
        NeoForge.EVENT_BUS.addListener(com.yearofice.food.debug.PreserveCommands::register);
    }
}
