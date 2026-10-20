package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:treat}: the client asks to hold a treatment on a part ({@link #ALL} for Treat all) or lets go ({@link #CANCEL}). */
public record TreatPayload(int part) implements CustomPacketPayload {
    public static final int ALL = -1, CANCEL = -2;

    public static final Type<TreatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "treat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TreatPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TreatPayload::part, TreatPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
