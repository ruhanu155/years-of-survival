package com.yearofice.net;

import com.yearofice.core.YearOfIce;
import com.yearofice.net.payload.ClockSyncPayload;
import com.yearofice.net.payload.EnvSyncPayload;
import com.yearofice.net.payload.HudLinePayload;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The shared layers' payloads (Networking; the studio lead owns {@code net}). Each client-bound payload has a sink
 * the client installs at start-up, so a dedicated server never loads a client class.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class YoiNet {
    private YoiNet() {}

    public static volatile Consumer<ClockSyncPayload> clockSink = p -> { };
    public static volatile Consumer<EnvSyncPayload> envSink = p -> { };
    public static volatile Consumer<HudLinePayload> hudSink = p -> { };

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(ClockSyncPayload.TYPE, ClockSyncPayload.STREAM_CODEC, (p, ctx) -> clockSink.accept(p));
        r.playToClient(EnvSyncPayload.TYPE, EnvSyncPayload.STREAM_CODEC, (p, ctx) -> envSink.accept(p));
        r.playToClient(HudLinePayload.TYPE, HudLinePayload.STREAM_CODEC, (p, ctx) -> hudSink.accept(p));
    }

    /** Sends to {@code p} if its connection knows the payload (GameTest mock players do not). */
    public static void send(ServerPlayer p, CustomPacketPayload payload) {
        if (p.connection != null && p.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(p, payload);
    }
}
