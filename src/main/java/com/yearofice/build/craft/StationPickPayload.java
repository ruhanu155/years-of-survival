package com.yearofice.build.craft;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:station_pick}, client to server: "start this recipe". The server judges it again before it starts. */
public record StationPickPayload(ResourceLocation recipe, boolean hasBench, BlockPos bench) implements CustomPacketPayload {
    public static final Type<StationPickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "station_pick"));
    public static final StreamCodec<FriendlyByteBuf, StationPickPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeResourceLocation(p.recipe);
                b.writeBoolean(p.hasBench);
                b.writeBlockPos(p.bench);
            },
            b -> new StationPickPayload(b.readResourceLocation(), b.readBoolean(), b.readBlockPos()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
