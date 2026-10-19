package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:clock_sync}, server to every player, every 20 ticks and on a rate change (Layer 1): the clock
 * now, the world rate and its ds a tick (so the client predicts between syncs), the day length, and this player's
 * own resolved rate. 20 bytes.
 */
public record ClockSyncPayload(long gameDs, int dsPerTick, byte worldRate, byte playerRate, short dayLengthMin)
        implements CustomPacketPayload {

    public static final Type<ClockSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "clock_sync"));

    public static final StreamCodec<ByteBuf, ClockSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ClockSyncPayload::gameDs,
            ByteBufCodecs.VAR_INT, ClockSyncPayload::dsPerTick,
            ByteBufCodecs.BYTE, ClockSyncPayload::worldRate,
            ByteBufCodecs.BYTE, ClockSyncPayload::playerRate,
            ByteBufCodecs.SHORT, ClockSyncPayload::dayLengthMin,
            ClockSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
