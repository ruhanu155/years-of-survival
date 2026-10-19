package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:drink}, client to server: "I used an empty hand on open water". Vanilla's empty-hand use never
 * reaches the server when the look ray ends in a fluid, so the client asks and the server checks the ray itself.
 */
public record DrinkPayload() implements CustomPacketPayload {
    public static final DrinkPayload INSTANCE = new DrinkPayload();
    public static final Type<DrinkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "drink"));
    public static final StreamCodec<ByteBuf, DrinkPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
