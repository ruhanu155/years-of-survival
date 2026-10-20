package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:pack_readout}, server to client: what the pack screen shows about the load. Strength and Hauling
 * practice are the server's, so the server states the three band edges (kg, already scaled by both) and the worn
 * pack's usable cap, and the screen draws them.
 *
 * @param strength   0-100
 * @param edgesKg    Light / Heavy / Loaded upper edges
 * @param capKg      the worn pack's usable limit, or the no-pack limit
 * @param band       {@link com.yearofice.sim.pack.LoadBands.Band} ordinal now
 */
public record PackReadoutPayload(float strength, float haulingReps, float edge0, float edge1, float edge2,
                                 float capKg, int band) implements CustomPacketPayload {
    public static final Type<PackReadoutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "pack_readout"));
    public static final StreamCodec<ByteBuf, PackReadoutPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeFloat(p.strength);
        b.writeFloat(p.haulingReps);
        b.writeFloat(p.edge0);
        b.writeFloat(p.edge1);
        b.writeFloat(p.edge2);
        b.writeFloat(p.capKg);
        ByteBufCodecs.VAR_INT.encode(b, p.band);
    }, b -> new PackReadoutPayload(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
            b.readFloat(), ByteBufCodecs.VAR_INT.decode(b)));

    /** The client installs this at start-up; a dedicated server never loads the client class. */
    public static volatile Consumer<PackReadoutPayload> sink = p -> { };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
