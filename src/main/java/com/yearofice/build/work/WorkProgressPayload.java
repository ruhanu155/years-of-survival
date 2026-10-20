package com.yearofice.build.work;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:work_progress}, server to client (Hold-to-work and the use key: "The server sends work_progress
 * every 5 ticks; the client predicts at the action's rate between them and snaps on each payload"). Drives the
 * {@code work_ring} HUD layer.
 *
 * @param share   the job's progress, 0–1
 * @param perTick the share it gains a tick, for prediction
 * @param phase   what the ring is filling for ({@link Phase})
 * @param done    counted steps (swings, rounds, strikes) and {@code of} how many
 */
public record WorkProgressPayload(boolean active, float share, float perTick, byte phase, short done, short of) implements CustomPacketPayload {
    public static final Type<WorkProgressPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "work_progress"));
    public static final StreamCodec<FriendlyByteBuf, WorkProgressPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeBoolean(p.active);
                b.writeFloat(p.share);
                b.writeFloat(p.perTick);
                b.writeByte(p.phase);
                b.writeShort(p.done);
                b.writeShort(p.of);
            },
            b -> new WorkProgressPayload(b.readBoolean(), b.readFloat(), b.readFloat(), b.readByte(), b.readShort(), b.readShort()));

    /** The ring's label. */
    public enum Phase { NOTCH, BACK_CUT, CUT_BLOCK, CUT_TOP, LIMB, BUCK, SPLIT, SET_BLOCK, CRAFT, LIFT, CHIP }

    public static WorkProgressPayload idle() { return new WorkProgressPayload(false, 0, 0, (byte) 0, (short) 0, (short) 0); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
