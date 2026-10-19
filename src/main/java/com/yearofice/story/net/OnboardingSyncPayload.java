package com.yearofice.story.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:onboarding_sync}: the owner's guided-night state for the objective beacon and the messenger's
 * Guide page. Sent on a change only. The server decides every field; the client draws.
 *
 * @param status       {@link com.yearofice.sim.story.FirstNight.Status} ordinal
 * @param step         {@link com.yearofice.sim.story.FirstNightStep} ordinal
 * @param beacon       the beacon is switched on for this world
 * @param hasTarget    whether {@code target} is set
 * @param target       the block the beacon stands on
 * @param nearFireM    the near-fire radius in whole metres, for the sleep line
 * @param sunsetMinute today's sunset as an absolute game minute, for the Guide page
 */
public record OnboardingSyncPayload(int status, int step, boolean beacon, boolean hasTarget, BlockPos target,
                                    int nearFireM, long sunsetMinute) implements CustomPacketPayload {

    public static final Type<OnboardingSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "onboarding_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OnboardingSyncPayload> STREAM_CODEC =
            StreamCodec.of(OnboardingSyncPayload::write, OnboardingSyncPayload::read);

    static void write(RegistryFriendlyByteBuf b, OnboardingSyncPayload p) {
        b.writeVarInt(p.status);
        b.writeVarInt(p.step);
        b.writeBoolean(p.beacon);
        b.writeBoolean(p.hasTarget);
        b.writeBlockPos(p.target);
        b.writeVarInt(p.nearFireM);
        b.writeVarLong(p.sunsetMinute);
    }

    static OnboardingSyncPayload read(RegistryFriendlyByteBuf b) {
        return new OnboardingSyncPayload(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBlockPos(),
                b.readVarInt(), b.readVarLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
