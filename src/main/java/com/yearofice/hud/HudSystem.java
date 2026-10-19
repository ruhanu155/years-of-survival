package com.yearofice.hud;

import com.yearofice.api.hud.HudBus;
import com.yearofice.core.YearOfIce;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Installs {@link HudBus} and flushes it at the end of every server tick, after the clock has stepped. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class HudSystem {
    private HudSystem() {}

    static final HudBusImpl BUS = new HudBusImpl();

    public static void init() { HudBus.install(BUS); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(ServerTickEvent.Post e) { BUS.flush(e.getServer()); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { BUS.forget(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { BUS.clear(); }
}
