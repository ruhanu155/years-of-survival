package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:decay_heads}, server to client (Minecraft development › Layer 5, Sync): a decay clock's head, its
 * D at a game minute, its rate in decay-hours per game hour and the air it reads, so the client computes every
 * freshness bar with the server's formula. The slice has one clock, handle 0 ({@code air:slice}); body, pocket and
 * container clocks join it in M5. 30 bytes.
 */
public record DecayHeadsPayload(short clock, double d, double minute, double ratePerHour, float airC)
        implements CustomPacketPayload {

    public static final Type<DecayHeadsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "decay_heads"));

    public static final StreamCodec<ByteBuf, DecayHeadsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT, DecayHeadsPayload::clock,
            ByteBufCodecs.DOUBLE, DecayHeadsPayload::d,
            ByteBufCodecs.DOUBLE, DecayHeadsPayload::minute,
            ByteBufCodecs.DOUBLE, DecayHeadsPayload::ratePerHour,
            ByteBufCodecs.FLOAT, DecayHeadsPayload::airC,
            DecayHeadsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
