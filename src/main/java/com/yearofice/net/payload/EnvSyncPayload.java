package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:env_sync}, server to each player once a game minute (Layer 2, Sync; Networking): the player's
 * own sample: air, feels-like, wind, the day's state, precipitation, snow here and visibility. Temperatures and
 * wind in tenths, snow in cm, visibility in metres. The forecast block joins it with the forecast (M7).
 */
public record EnvSyncPayload(short airC10, short feelsC10, short windMs10, short gustMs10, short windFromDeg,
                             byte dayState, byte precip, short precipMmH10, short snowCm, int visibilityM, short waterC10)
        implements CustomPacketPayload {

    public static final Type<EnvSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "env_sync"));

    public static final StreamCodec<ByteBuf, EnvSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnvSyncPayload decode(ByteBuf b) {
            return new EnvSyncPayload(b.readShort(), b.readShort(), b.readShort(), b.readShort(), b.readShort(),
                    b.readByte(), b.readByte(), b.readShort(), b.readShort(), ByteBufCodecs.VAR_INT.decode(b), b.readShort());
        }

        @Override
        public void encode(ByteBuf b, EnvSyncPayload p) {
            b.writeShort(p.airC10).writeShort(p.feelsC10).writeShort(p.windMs10).writeShort(p.gustMs10)
                    .writeShort(p.windFromDeg).writeByte(p.dayState).writeByte(p.precip).writeShort(p.precipMmH10)
                    .writeShort(p.snowCm);
            ByteBufCodecs.VAR_INT.encode(b, p.visibilityM);
            b.writeShort(p.waterC10);
        }
    };

    public double airC() { return airC10 / 10.0; }

    public double feelsLikeC() { return feelsC10 / 10.0; }

    public double windMs() { return windMs10 / 10.0; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
