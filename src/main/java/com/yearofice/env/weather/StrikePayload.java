package com.yearofice.env.weather;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:strike}, server to players within 12 km: where the bolt landed, its seed and power. The client draws the flash and plays the sound. */
public record StrikePayload(BlockPos pos, int seed, float power) implements CustomPacketPayload {
    public static final Type<StrikePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "strike"));
    public static final StreamCodec<ByteBuf, StrikePayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, StrikePayload::pos,
            ByteBufCodecs.INT, StrikePayload::seed, ByteBufCodecs.FLOAT, StrikePayload::power, StrikePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
