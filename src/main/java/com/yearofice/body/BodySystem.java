package com.yearofice.body;

import com.yearofice.api.body.BodyService;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The body system's entry: registers the attachment, the messenger item and its component, and installs
 * {@link BodyService}. Listeners subscribe themselves ({@code @EventBusSubscriber}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodySystem {
    private BodySystem() {}

    public static void init(IEventBus modBus) {
        BodyAttachments.register(modBus);
        MessengerItems.register(modBus);
        BodyService.install(new BodyServiceImpl());
        com.yearofice.body.sleep.Sleep.init();
        com.yearofice.body.injury.InjuryRegistry.register(modBus);
    }

    /**
     * {@code copyOnDeath} already carried the record over; a death is never a normal end in Endless, so the copy
     * is set to the return body by the SOS rules (Collapse and SOS, step 5).
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer p)) return;
        BodyState s = BodyAttachments.of(p);
        if (s.sos == 0) BodyModel.sosReturn(s, BodyConfig.tuning());
    }
}
