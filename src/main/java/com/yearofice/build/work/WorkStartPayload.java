package com.yearofice.build.work;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:work_start}, client to server (Player systems and client › Hold-to-work and the use key): "use is
 * held on this target". Sent when the hold passes 0.25 s and every 5 ticks while it lasts, as the server's hold
 * check; {@code holding = false} says the key came up. The server picks the job and re-checks everything.
 *
 * @param entity the target entity's id, or -1 for a block target at {@code pos}
 * @param hitX   where the crosshair meets the target (the splitting strike's aim)
 */
public record WorkStartPayload(boolean holding, BlockPos pos, int entity, byte face, float hitX, float hitY, float hitZ)
        implements CustomPacketPayload {
    public static final Type<WorkStartPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "work_start"));
    public static final StreamCodec<FriendlyByteBuf, WorkStartPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeBoolean(p.holding);
                b.writeBlockPos(p.pos);
                b.writeVarInt(p.entity);
                b.writeByte(p.face);
                b.writeFloat(p.hitX);
                b.writeFloat(p.hitY);
                b.writeFloat(p.hitZ);
            },
            b -> new WorkStartPayload(b.readBoolean(), b.readBlockPos(), b.readVarInt(), b.readByte(), b.readFloat(), b.readFloat(), b.readFloat()));

    public static WorkStartPayload release() { return new WorkStartPayload(false, BlockPos.ZERO, -1, (byte) 0, 0, 0, 0); }

    /** The target's identity: a hold on the same target keeps the same job. */
    public long targetKey() { return entity >= 0 ? (1L << 62) | entity : pos.asLong() ^ ((long) face << 58); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
