package com.yearofice.hunt.fishing;

import com.yearofice.hunt.fishing.debug.FishingCommands;
import com.yearofice.hunt.fishing.net.FishingNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Wires {@code hunt.fishing}: registries, payloads, {@code /yoi fish}, the close clock, worm digging and config
 * reloads. Called once from the mod constructor.
 */
public final class FishingSystem {
    private FishingSystem() {}

    public static void init(IEventBus modBus) {
        FishingRegistry.register(modBus);
        modBus.addListener(FishingNetwork::register);
        modBus.addListener((ModConfigEvent.Loading e) -> FishingConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> FishingConfig.invalidate());
        modBus.addListener(FishingSystem::creativeTabs);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
            for (var t : FishingRegistry.FISH_TYPES.values()) e.put(t.get(), FishEntity.attributes().build());
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof net.minecraft.server.level.ServerLevel sl && sl.dimension() == net.minecraft.world.level.Level.OVERWORLD) FishSpawner.tick(sl);
        });
        NeoForge.EVENT_BUS.addListener(FishingCommands::register);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> FishingClock.afterLevelTick(e.getLevel()));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> WormDigging.onBreak(e));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> FishingService.clear());
    }

    /** Until the studio adds a Year of Ice tab, the rod kit sits in the vanilla tabs. */
    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(FishingRegistry.ROD.get());
            e.accept(FishingRegistry.SPOON.get());
            e.accept(FishingRegistry.FLY.get());
            e.accept(FishingRegistry.SET_LINE_ITEM.get());
            e.accept(FishingRegistry.WEIR_BASKET_ITEM.get());
            e.accept(FishingRegistry.GILL_NET_SEGMENT.get());
        } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(FishingRegistry.WORM.get());
            e.accept(FishingRegistry.ROE.get());
            e.accept(FishingRegistry.FISH_CHUNK.get());
        } else if (e.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            e.accept(FishingService.landedFish(600));
        }
    }
}
