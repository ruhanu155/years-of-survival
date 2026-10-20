package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:distant_sound} (Minecraft engine › Danger at range): a sound whose range is over 128 blocks (a
 * howl at 3 km) goes to every player inside it as this payload; the client plays it with its own distance curve,
 * direction and delay. Server to client, about 16 bytes.
 *
 * @param sound  the call or sound id, e.g. {@code yearofice:entity.wolf.howl}
 * @param x      source position
 * @param rangeM audible range in metres
 * @param gameTick the server tick it sounded
 */
public record DistantSoundPayload(String sound, double x, double y, double z, float rangeM, long gameTick) implements CustomPacketPayload {
    public static final Type<DistantSoundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "distant_sound"));

    public static final StreamCodec<ByteBuf, DistantSoundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), DistantSoundPayload::sound,
            ByteBufCodecs.DOUBLE, DistantSoundPayload::x,
            ByteBufCodecs.DOUBLE, DistantSoundPayload::y,
            ByteBufCodecs.DOUBLE, DistantSoundPayload::z,
            ByteBufCodecs.FLOAT, DistantSoundPayload::rangeM,
            ByteBufCodecs.VAR_LONG, DistantSoundPayload::gameTick,
            DistantSoundPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
