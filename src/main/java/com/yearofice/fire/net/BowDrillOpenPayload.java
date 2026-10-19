package com.yearofice.fire.net;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: open the bow-drill screen at a fire ring (Player systems and client › Other screens: "RMB hold
 * with the kit at a cleared spot"). Carries what the screen needs to run the minigame honestly: whether the board is
 * wet (heat caps at 60), the ember's life (halved in wind without shelter) and whether the player is Practiced (one
 * 6-second hold).
 */
public record BowDrillOpenPayload(BlockPos pos, boolean boardWet, int emberLifeTicks, boolean practiced) implements CustomPacketPayload {
    public static final Type<BowDrillOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "bow_drill_open"));
    public static final StreamCodec<ByteBuf, BowDrillOpenPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BowDrillOpenPayload::pos,
            ByteBufCodecs.BOOL, BowDrillOpenPayload::boardWet,
            ByteBufCodecs.VAR_INT, BowDrillOpenPayload::emberLifeTicks,
            ByteBufCodecs.BOOL, BowDrillOpenPayload::practiced,
            BowDrillOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
