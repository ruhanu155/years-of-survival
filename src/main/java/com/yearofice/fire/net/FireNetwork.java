package com.yearofice.fire.net;

import com.yearofice.fire.BowDrillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The fire team's payloads: {@code yearofice:bow_drill_open} (server to client) and {@code yearofice:minigame_result}
 * (client to server). The client handler is a lambda into {@code com.yearofice.client}, so a dedicated server
 * never loads a client class.
 */
public final class FireNetwork {
    private FireNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(BowDrillOpenPayload.TYPE, BowDrillOpenPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.yearofice.client.minigame.bowdrill.BowDrillClient.open(p)));
        r.playToServer(MinigameResultPayload.TYPE, MinigameResultPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) BowDrillService.onResult(sp, p);
        }));
    }
}
