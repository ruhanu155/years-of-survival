package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:sos_line}: the "why I left" line (empty when skipped) from the flight screen. */
public record SosLinePayload(String line) implements CustomPacketPayload {
    public static final Type<SosLinePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "sos_line"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SosLinePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SosLinePayload::line, SosLinePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
