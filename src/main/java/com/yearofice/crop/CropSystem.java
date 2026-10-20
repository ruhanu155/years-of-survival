package com.yearofice.crop;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Wires the garden: blocks and items, crop data, the harvest and card hooks, the day pass and {@code /yoi crop}. */
public final class CropSystem {
    private CropSystem() {}

    public static void init(IEventBus modBus) {
        CropRegistry.register(modBus);
        NeoForge.EVENT_BUS.addListener(CropData::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(CropEvents::onRightClick);
        NeoForge.EVENT_BUS.addListener(CropEvents::onDay);
        NeoForge.EVENT_BUS.addListener(CropCommands::register);
    }
}
