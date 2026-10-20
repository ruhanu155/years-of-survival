package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:sos_flight}: opens the SOS flight screen ("SOS sent 14:22, day 143"), or the run-ended screen. */
public record SosFlightPayload(String cause, String when, int day, boolean runEnded) implements CustomPacketPayload {
    public static final Type<SosFlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "sos_flight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SosFlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SosFlightPayload::cause, ByteBufCodecs.STRING_UTF8, SosFlightPayload::when,
            ByteBufCodecs.VAR_INT, SosFlightPayload::day, ByteBufCodecs.BOOL, SosFlightPayload::runEnded, SosFlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
