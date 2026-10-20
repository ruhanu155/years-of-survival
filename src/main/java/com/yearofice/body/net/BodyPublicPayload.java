package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:body_public}: the small packet other players see (Body tab › body_public): what can be seen from
 * across a clearing, never a number. Entity id and one flag byte.
 */
public record BodyPublicPayload(int entityId, int flags) implements CustomPacketPayload {
    public static final int BLEEDING = 1, LIMP = 2, ARM_HURT = 4, DOWN = 8, FEVER = 16, SPLINTED = 32;

    public static final Type<BodyPublicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "body_public"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyPublicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BodyPublicPayload::entityId, ByteBufCodecs.VAR_INT, BodyPublicPayload::flags,
            BodyPublicPayload::new);

    public boolean has(int flag) { return (flags & flag) != 0; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
