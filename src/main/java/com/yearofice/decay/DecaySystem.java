package com.yearofice.decay;

import com.yearofice.api.decay.DecayService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Wires the slice's Decay and freshness layer: the service and {@code decay/} curves. Called once from the mod constructor. */
public final class DecaySystem {
    private DecaySystem() {}

    public static void init(IEventBus modBus) {
        DecayService.install(new DecayServiceImpl());
        NeoForge.EVENT_BUS.addListener(DecayCurves::onAddReloadListeners);
    }
}
