package com.yearofice.ecosystem.companion;

import com.yearofice.ecosystem.herd.LeadFence;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** M9 lane A wiring, called once from {@code EcosystemSystem.init}. */
public final class CompanionSystem {
    private CompanionSystem() {}

    public static void init(IEventBus modBus) {
        CompanionRegistry.register(modBus);
        CompanionAttachments.register(modBus);
        NeoForge.EVENT_BUS.addListener(Companions::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(DenPups::onDay);
        NeoForge.EVENT_BUS.addListener(LeadFence::onTick);
        NeoForge.EVENT_BUS.addListener(LeadFence::onDeath);
        NeoForge.EVENT_BUS.addListener(CompanionCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> LeadFence.clear());
    }
}
