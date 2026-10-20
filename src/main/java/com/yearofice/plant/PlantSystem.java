package com.yearofice.plant;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Wires plants, foraging, knowledge and the mineral lick. Called once from the mod constructor. */
public final class PlantSystem {
    private PlantSystem() {}

    public static void init(IEventBus modBus) {
        PlantRegistry.register(modBus);
        PlantAttachments.register(modBus);
        LickRegistry.register(modBus);
        NeoForge.EVENT_BUS.addListener(PlantData::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(PlantEvents::onRightClick);
        NeoForge.EVENT_BUS.addListener(PlantEvents::onEaten);
        NeoForge.EVENT_BUS.addListener(PlantEvents::onDay);
        NeoForge.EVENT_BUS.addListener(PlantPoison::onMinute);
        NeoForge.EVENT_BUS.addListener(PlantCommands::register);
        NeoForge.EVENT_BUS.addListener(LickCrust::onRightClick);
        NeoForge.EVENT_BUS.addListener(LickCrust::onDay);
    }
}
