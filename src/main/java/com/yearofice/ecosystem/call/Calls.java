package com.yearofice.ecosystem.call;

import com.yearofice.net.payload.DistantSoundPayload;
import com.yearofice.sim.ecosystem.CallRules;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Animal vocalisations (M5 debt: calls). A call heard beyond vanilla's 128 blocks goes as the
 * {@code yearofice:distant_sound} payload to every player in range ({@link CallRules} holds the ranges and the
 * calendar); inside 128 blocks the sound bus plays the sound event itself once the Look lane registers it. The client
 * sink collects what it was sent for the caption and the audio engine.
 */
@EventBusSubscriber(modid = com.yearofice.core.YearOfIce.MOD_ID)
public final class Calls {
    private Calls() {}

    public static final double VANILLA_RANGE_M = 128;
    /** The client installs its player; a dedicated server leaves the no-op. */
    public static volatile Consumer<DistantSoundPayload> clientSink = p -> { };

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(DistantSoundPayload.TYPE, DistantSoundPayload.STREAM_CODEC, (p, ctx) -> clientSink.accept(p));
    }

    /** Sounds a call at {@code at}; returns how many players were sent the distant payload. */
    public static int emit(ServerLevel level, Vec3 at, String sound, double rangeM) {
        int n = 0;
        if (rangeM <= VANILLA_RANGE_M) return 0;
        DistantSoundPayload pl = new DistantSoundPayload(sound, at.x, at.y, at.z, (float) rangeM, level.getGameTime());
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(at) > rangeM * rangeM) continue;
            PacketDistributor.sendToPlayer(p, pl);
            n++;
        }
        return n;
    }

    public static int emit(ServerLevel level, BlockPos at, String sound, double rangeM) { return emit(level, Vec3.atCenterOf(at), sound, rangeM); }
}
