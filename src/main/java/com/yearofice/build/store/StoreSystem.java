package com.yearofice.build.store;

import com.yearofice.api.build.CacheAccess;
import com.yearofice.api.build.StoreService;
import com.yearofice.build.cellar.CellarIndex;
import com.yearofice.build.cellar.CellarRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Wires the storage blocks, the cellars and the {@code yearofice:store} data map; called from the building system. */
public final class StoreSystem {
    private StoreSystem() {}

    public static void init(IEventBus modBus) {
        StoreRegistry.register(modBus);
        CellarRegistry.register(modBus);
        com.yearofice.build.multiblock.MultiblockData.init();
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(StoreData.MAP));
        modBus.addListener(StoreSystem::creativeTabs);
        StoreService.INSTANCE.set(StoreClimate.INSTANCE);
        CacheAccess.INSTANCE.set(StoreClimate.INSTANCE);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> CellarIndex.clear());
        NeoForge.EVENT_BUS.addListener(StoreCommands::register);
        SmokeClock.init();
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() != CreativeModeTabs.FUNCTIONAL_BLOCKS) return;
        e.accept(StoreRegistry.CRATE_ITEM.get());
        e.accept(StoreRegistry.BARREL_ITEM.get());
        e.accept(StoreRegistry.SHELF_ITEM.get());
        e.accept(StoreRegistry.HANGING_BAR_ITEM.get());
        e.accept(StoreRegistry.CACHE_LEGS_ITEM.get());
        e.accept(StoreRegistry.CACHE_POLE_ITEM.get());
        e.accept(StoreRegistry.PLACED_LANTERN_ITEM.get());
        e.accept(StoreRegistry.SMOKEHOUSE_ITEM.get());
        e.accept(StoreRegistry.FIREWOOD_SHED_ITEM.get());
        e.accept(CellarRegistry.ROOT_CELLAR_ITEM.get());
        e.accept(CellarRegistry.PERMAFROST_CELLAR_ITEM.get());
    }
}
