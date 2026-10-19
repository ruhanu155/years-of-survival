package com.yearofice.story.net;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The story's payloads. The client installs {@link #clientSink}; a dedicated server never touches client code. */
public final class StoryNet {
    private StoryNet() {}

    /** Receives {@code onboarding_sync} on the client. */
    public static volatile Consumer<OnboardingSyncPayload> clientSink = p -> { };

    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(OnboardingSyncPayload.TYPE, OnboardingSyncPayload.STREAM_CODEC, (p, ctx) -> clientSink.accept(p));
    }

    /** Sends to the owner if its connection knows the payload (test players do not). */
    public static void sendToOwner(ServerPlayer p, OnboardingSyncPayload payload) {
        if (p.connection != null && p.connection.hasChannel(OnboardingSyncPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
