package com.yearofice.fire.stove;

import com.yearofice.api.fire.StoveHeatLookup;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Wires the stoves: registry, the heat lookup lane A reads, the debug commands. Called from the fire system. */
public final class StoveSystem {
    private StoveSystem() {}

    public static void init(IEventBus modBus) {
        StoveRegistry.register(modBus);
        StoveHeatLookup.INSTANCE.set(StoveIndex.INSTANCE);
        modBus.addListener(StoveSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener(StoveCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> StoveIndex.INSTANCE.clear());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() != CreativeModeTabs.FUNCTIONAL_BLOCKS) return;
        e.accept(StoveRegistry.TIN_STOVE_ITEM.get());
        e.accept(StoveRegistry.BARREL_STOVE_ITEM.get());
        e.accept(StoveRegistry.MASONRY_STOVE_ITEM.get());
        e.accept(StoveRegistry.CHIMNEY_PIPE_ITEM.get());
    }
}
