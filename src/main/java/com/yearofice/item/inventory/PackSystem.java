package com.yearofice.item.inventory;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.item.LoadService;
import com.yearofice.item.ItemExpiry;
import com.yearofice.item.YoiDataMaps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Wires the pack: items, the attachment, the data maps, {@link LoadService}, the tick, pickup and item expiry. */
public final class PackSystem {
    private PackSystem() {}

    static final LoadCalculator LOAD = new LoadCalculator();

    public static void init(IEventBus modBus) {
        PackItems.register(modBus);
        KitItems.register(modBus);
        PackAttachments.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> YoiDataMaps.register(e));
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(PackItems.CRATE_BACKPACK.get());
        });
        LoadService.install(LOAD);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !p.isSpectator()) PackSync.tick(p);
        });
        NeoForge.EVENT_BUS.addListener(Pickup::onPickupPre);
        NeoForge.EVENT_BUS.addListener(ItemExpiry::onJoin);
        NeoForge.EVENT_BUS.addListener((ClockEvent.Hour e) -> {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) ItemExpiry.sweep(server);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> PackSync.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> PackSync.clear());
        NeoForge.EVENT_BUS.addListener(PackCommands::register);
    }

}
