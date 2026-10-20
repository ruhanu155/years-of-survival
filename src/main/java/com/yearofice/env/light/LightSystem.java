package com.yearofice.env.light;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.env.LightData;
import com.yearofice.api.env.LightState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Wires {@code env.light}: the light data map and the {@code yearofice:light_state} component, and the per-player
 * refresh of carried lights every 10 ticks. Called once from {@code FireSystem.init} (the fire package owns the light
 * items; this package owns the field they feed).
 */
public final class LightSystem {
    private LightSystem() {}

    public static final int REFRESH_TICKS = 10;

    public static void init(IEventBus modBus) {
        LightState.COMPONENTS.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(LightData.MAP));
        NeoForge.EVENT_BUS.addListener(LightSystem::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> HeldLights.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> HeldLights.clear());
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % REFRESH_TICKS != 0) return;
        double air = EnvService.installed() ? EnvService.get().airC(p.serverLevel(), p.blockPosition()) : 0;
        HeldLights.refresh(p, air);
    }
}
