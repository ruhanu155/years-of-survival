package com.yearofice.body;

import com.yearofice.api.fire.FireEffortEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyModel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The body's side of the fire API: each {@link FireEffortEvent} (a bow-drill try, 60 kcal) is charged to the
 * calorie ledger. The try's 6–9 game minutes are the clock's to run (time-lapse at the close clock); the body steps
 * them like any other minutes when they pass. Fire heat reaches warmth through
 * {@link com.yearofice.api.fire.FireHeatLookup} in the environment sampler.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class FireHooks {
    private FireHooks() {}

    @SubscribeEvent
    public static void onEffort(FireEffortEvent e) {
        if (!(e.player() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        long minute = BodyTicker.clock.minute(p.server);
        BodyModel.charge(BodyAttachments.of(p), minute, e.kcal());
        BodyTicker.sync(p, minute, false);
    }
}
