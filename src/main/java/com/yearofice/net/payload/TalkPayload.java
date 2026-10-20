package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:talk}, client to server: {@code walk} is the double-tap V toggle for talk as you walk (B7). */
public record TalkPayload(boolean walk) implements CustomPacketPayload {
    public static final Type<TalkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "talk"));
    public static final StreamCodec<ByteBuf, TalkPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(TalkPayload::new, TalkPayload::walk);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
