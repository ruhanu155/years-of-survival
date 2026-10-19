package com.yearofice.hunt.fishing.net;

import com.yearofice.hunt.fishing.FishingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The rod's payloads: {@code yearofice:fish_input} (client to server), {@code yearofice:fish_state} and
 * {@code yearofice:fish_result} (server to client). Client handlers are lambdas into {@code com.yearofice.client},
 * so a dedicated server never loads a client class.
 */
public final class FishingNetwork {
    private FishingNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(FishInputPayload.TYPE, FishInputPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) FishingService.onInput(sp, p);
        }));
        r.playToClient(FishStatePayload.TYPE, FishStatePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.yearofice.client.minigame.fishing.FishingClient.onState(p)));
        r.playToClient(FishResultPayload.TYPE, FishResultPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.yearofice.client.minigame.fishing.FishingClient.onResult(p)));
    }
}
