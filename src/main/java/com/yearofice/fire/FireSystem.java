package com.yearofice.fire;

import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.fire.debug.FireCommands;
import com.yearofice.fire.net.FireNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Wires the fire package: registries, the fuel data map, payloads, {@code /yoi fire}, the heat lookup and config
 * reloads. Called once from the mod constructor.
 */
public final class FireSystem {
    private FireSystem() {}

    public static void init(IEventBus modBus) {
        FireRegistry.register(modBus);
        modBus.addListener(FireParts::registerDataMaps);
        modBus.addListener(FireNetwork::register);
        modBus.addListener((ModConfigEvent.Loading e) -> FireConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> FireConfig.invalidate());
        modBus.addListener(FireSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener(FireCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            FireIndex.INSTANCE.clear();
            FireEnv.clear();
            BowDrillService.clear();
        });
        FireHeatLookup.INSTANCE.set(FireIndex.INSTANCE);
    }

    /** Until the studio adds a Year of Ice tab, the fire kit sits in the vanilla tabs. */
    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(FireRegistry.BOW_DRILL.get());
            e.accept(FireRegistry.MATCH.get());
        } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(FireRegistry.TINDER_BUNDLE.get());
            e.accept(FireRegistry.KINDLING.get());
        } else if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(FireRegistry.FIRE_RING_ITEM.get());
        }
    }
}
