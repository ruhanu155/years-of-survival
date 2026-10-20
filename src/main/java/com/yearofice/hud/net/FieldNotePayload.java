package com.yearofice.hud.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** {@code yearofice:field_note}: show card {@code note} now. {@code firstYear} serves the "First year only" option. */
public record FieldNotePayload(String note, boolean firstYear) implements CustomPacketPayload {
    public static final Type<FieldNotePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "field_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldNotePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FieldNotePayload::note, ByteBufCodecs.BOOL, FieldNotePayload::firstYear,
            FieldNotePayload::new);

    @Override public Type<FieldNotePayload> type() { return TYPE; }
}
