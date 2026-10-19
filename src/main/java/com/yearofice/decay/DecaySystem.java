package com.yearofice.decay;

import com.yearofice.api.decay.DecayService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Wires the slice's Decay and freshness layer: the service, {@code decay/} curves and the air clock with its
 * {@code decay_heads} sync. Called once from the mod constructor.
 */
public final class DecaySystem {
    private DecaySystem() {}

    public static void init(IEventBus modBus) {
        DecayService.install(new DecayServiceImpl());
        NeoForge.EVENT_BUS.addListener(DecayCurves::onAddReloadListeners);
        DecayAir.init();
        com.yearofice.net.YoiNet.decaySink = DecayAir::acceptHead;
    }
}
