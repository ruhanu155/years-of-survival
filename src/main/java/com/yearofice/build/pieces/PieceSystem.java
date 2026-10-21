package com.yearofice.build.pieces;

import com.yearofice.build.rooms.Rooms;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Wires pieces, rooms, paths, stamping and their commands (M8 lane A). */
public final class PieceSystem {
    private PieceSystem() {}

    public static void init() {
        PathSpeed.init();
        PieceJobs.init();
        Rooms.init();
        NeoForge.EVENT_BUS.addListener(PieceCommands::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> Rooms.reset());
    }

    public static void creative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) PieceRegistry.ITEMS.getEntries().forEach(i -> e.accept(i.get()));
    }
}
