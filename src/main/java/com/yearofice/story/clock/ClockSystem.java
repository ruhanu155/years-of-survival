package com.yearofice.story.clock;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Installs {@link ClockService} at mod construction and drives it from the server's events. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class ClockSystem {
    private ClockSystem() {}

    static final ClockServiceImpl CLOCK = new ClockServiceImpl();

    public static void init() { ClockService.install(CLOCK); }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new ClockResources()); }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) { CLOCK.start(e.getServer()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { CLOCK.stop(); }

    /** Low priority: every system's own Post work this tick has asked its rates first. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTick(ServerTickEvent.Post e) { CLOCK.tick(); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) CLOCK.sync(p);
    }
}
