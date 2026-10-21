package com.yearofice.build;

import com.yearofice.api.build.BedData;
import com.yearofice.build.debug.BuildCommands;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Wires the building package for the slice: the bough bed and spruce boughs, the {@code yearofice:bed} block data map
 * ({@link BedData}) and {@code /yoi bed}. The camp classes subscribe their own game listeners. Called once from the
 * mod constructor.
 */
public final class BuildSystem {
    private BuildSystem() {}

    public static void init(IEventBus modBus) {
        BuildRegistry.register(modBus);
        com.yearofice.build.store.StoreSystem.init(modBus);
        com.yearofice.build.craft.CraftSystem.init(modBus);
        // A lifted box's hold (A7) is checked before the wood jobs'.
        var woodResolver = com.yearofice.build.work.WorkService.resolver;
        com.yearofice.build.work.WorkService.resolver = (p, w) -> {
            var box = com.yearofice.item.inventory.BoxCarry.resolve(p, w);
            return box != null ? box : woodResolver.apply(p, w);
        };
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(BedData.MAP));
        modBus.addListener(BuildSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener(BuildCommands::register);
        NeoForge.EVENT_BUS.addListener(com.yearofice.build.dugout.DugoutCommands::register);
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) e.accept(BuildRegistry.SPRUCE_BOUGHS.get());
    }
}
