package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:pickup}, client to server (Networking): "pick up the item entity my ray hit". The server checks
 * reach, weight and open rows itself; the client never claims a weight.
 */
public record PickupPayload(int entityId) implements CustomPacketPayload {
    public static final Type<PickupPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "pickup"));
    public static final StreamCodec<ByteBuf, PickupPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(PickupPayload::new, PickupPayload::entityId);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
