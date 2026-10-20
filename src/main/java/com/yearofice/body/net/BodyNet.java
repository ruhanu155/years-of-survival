package com.yearofice.body.net;

import com.yearofice.body.intake.Drinking;
import com.yearofice.core.YearOfIce;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The body's payloads. The client installs {@link #clientSink}; a dedicated server never touches client code. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodyNet {
    private BodyNet() {}

    /** Receives {@code body_sync} on the client. */
    public static volatile Consumer<BodySyncPayload> clientSink = p -> { };

    /** Receives {@code sos_flight} on the client. */
    public static volatile Consumer<SosFlightPayload> sosSink = p -> { };

    /** Receives {@code body_public} for another player on the client. */
    public static volatile Consumer<BodyPublicPayload> publicSink = p -> { };

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(BodySyncPayload.TYPE, BodySyncPayload.STREAM_CODEC, (p, ctx) -> clientSink.accept(p));
        r.playToClient(SosFlightPayload.TYPE, SosFlightPayload.STREAM_CODEC, (p, ctx) -> sosSink.accept(p));
        r.playToServer(SosLinePayload.TYPE, SosLinePayload.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) com.yearofice.body.SosFlow.onLine(sp, p.line());
        });
        r.playToClient(BodyPublicPayload.TYPE, BodyPublicPayload.STREAM_CODEC, (p, ctx) -> publicSink.accept(p));
        r.playToServer(TreatPayload.TYPE, TreatPayload.STREAM_CODEC, (p, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (p.part() == TreatPayload.CANCEL) com.yearofice.body.injury.Treat.cancel(sp);
            else com.yearofice.body.injury.Treat.start(sp, p.part());
        });
        r.playToServer(DrinkPayload.TYPE, DrinkPayload.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Drinking.tryDrinkLookedAt(sp);
        });
    }

    public static void sendSos(ServerPlayer p, SosFlightPayload payload) {
        if (p.connection != null && p.connection.hasChannel(SosFlightPayload.TYPE)) PacketDistributor.sendToPlayer(p, payload);
    }

    /** Sends to the owner if its connection knows the payload (GameTest mock players do not). */
    public static void sendToOwner(ServerPlayer p, BodySyncPayload payload) {
        if (p.connection != null && p.connection.hasChannel(BodySyncPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
