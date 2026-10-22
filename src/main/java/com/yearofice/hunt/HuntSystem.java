package com.yearofice.hunt;

import com.yearofice.api.hunt.ConcealmentService;
import com.yearofice.hunt.debug.HuntCommands;
import com.yearofice.hunt.wound.WoundTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Wires the hunt (weapons, wounds, blood, calls, blinds): registries, the wound tick, concealment and {@code /yoi hunt}. Fishing is {@code hunt.fishing}. */
public final class HuntSystem {
    private HuntSystem() {}

    public static void init(IEventBus modBus) {
        HuntRegistry.register(modBus);
        com.yearofice.hunt.sign.SignRegistry.register(modBus);
        ConcealmentService.install(new HuntConcealment());
        modBus.addListener((ModConfigEvent.Loading e) -> HuntConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> HuntConfig.invalidate());
        modBus.addListener(HuntSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> { if (e.getLevel() instanceof ServerLevel sl) WoundTracker.tick(sl); });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getEntity() instanceof LivingEntity le) WoundTracker.onJoin(sl, le);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> WoundTracker.clear());
        NeoForge.EVENT_BUS.addListener(HuntCommands::register);
        NeoForge.EVENT_BUS.addListener(com.yearofice.hunt.sign.SignCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> com.yearofice.hunt.sign.SignService.clearAll());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.COMBAT) {
            e.accept(HuntRegistry.SELF_BOW.get());
            e.accept(HuntRegistry.KIT_BOW.get());
            e.accept(HuntRegistry.LONGBOW.get());
            e.accept(HuntRegistry.SINEW_BOW.get());
            e.accept(HuntRegistry.SPEAR.get());
            for (String h : new String[] {"knapped_stone", "bone", "steel", "blunt"}) e.accept(HuntItems.arrow(h, 1));
            e.accept(HuntRegistry.ARROWHEAD.get());
        } else if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(HuntRegistry.MOOSE_CALL.get());
            e.accept(HuntRegistry.SQUEAK_CALL.get());
        } else if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            e.accept(HuntRegistry.POLE_BLIND_ITEM.get());
            e.accept(HuntRegistry.TREE_STAND_ITEM.get());
        }
    }
}
