package com.yearofice.net.payload;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:encounter_clip}, server to owner: a hand clip to play ({@code ""} ends the encounter). */
public record EncounterClipPayload(String clip) implements CustomPacketPayload {
    public static final Type<EncounterClipPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "encounter_clip"));
    public static final StreamCodec<ByteBuf, EncounterClipPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(32).map(EncounterClipPayload::new, EncounterClipPayload::clip);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
