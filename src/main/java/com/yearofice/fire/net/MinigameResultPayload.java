package com.yearofice.fire.net;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fire.BowDrillJudge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: one minigame attempt's summary (Minecraft development › Payloads: {@code minigame_result},
 * 30–200 B, per attempt). For the bow drill: how it ended, strokes, strokes on the beat, peak heat, good puffs and
 * whether the Practiced hold was used. The server measures the duration itself and judges the rest.
 */
public record MinigameResultPayload(String minigame, BlockPos pos, int claim, int strokes, int onBeat, int peakHeat,
                                    int goodPuffs, boolean held) implements CustomPacketPayload {
    public static final String BOW_DRILL = "bow_drill";
    public static final Type<MinigameResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "minigame_result"));
    public static final StreamCodec<ByteBuf, MinigameResultPayload> CODEC = StreamCodec.of(MinigameResultPayload::write, MinigameResultPayload::read);

    private static void write(ByteBuf buf, MinigameResultPayload p) {
        ByteBufCodecs.stringUtf8(32).encode(buf, p.minigame);
        BlockPos.STREAM_CODEC.encode(buf, p.pos);
        for (int v : new int[] {p.claim, p.strokes, p.onBeat, p.peakHeat, p.goodPuffs}) ByteBufCodecs.VAR_INT.encode(buf, v);
        ByteBufCodecs.BOOL.encode(buf, p.held);
    }

    private static MinigameResultPayload read(ByteBuf buf) {
        String m = ByteBufCodecs.stringUtf8(32).decode(buf);
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
        int[] v = new int[5];
        for (int i = 0; i < 5; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
        boolean held = ByteBufCodecs.BOOL.decode(buf);
        return new MinigameResultPayload(m, pos, v[0], v[1], v[2], v[3], v[4], held);
    }

    public BowDrillJudge.Claim claimValue() {
        BowDrillJudge.Claim[] v = BowDrillJudge.Claim.values();
        return claim >= 0 && claim < v.length ? v[claim] : BowDrillJudge.Claim.ABORTED;
    }

    public BowDrillJudge.Summary summary() {
        return new BowDrillJudge.Summary(claimValue(), Math.max(0, strokes), Math.max(0, onBeat), Math.max(0, peakHeat),
                Math.max(0, goodPuffs), held);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
