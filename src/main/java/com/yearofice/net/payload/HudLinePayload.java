package com.yearofice.net.payload;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:hud_line}, server to owner, one per line (Layer 8; Networking: about 40 B): the type, priority,
 * lang key and typed arguments. {@code clear} withdraws the prompt offered under {@code key}.
 */
public record HudLinePayload(byte kind, byte priority, String key, List<HudArg> args, boolean clear) implements CustomPacketPayload {
    /** The source tag {@code HudBus.clearPrompt} uses for a withdrawal. */
    public static final String CLEAR = "clear";

    public static final Type<HudLinePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "hud_line"));

    public static HudLinePayload of(HudLine l) {
        return new HudLinePayload((byte) l.type().ordinal(), (byte) l.priority(), l.key(), l.args(), CLEAR.equals(l.source()));
    }

    public HudLineType lineType() { return HudLineType.values()[kind]; }

    static final StreamCodec<ByteBuf, HudArg> ARG = new StreamCodec<>() {
        @Override
        public HudArg decode(ByteBuf b) {
            HudArg.Kind k = HudArg.Kind.values()[b.readByte()];
            long v = ByteBufCodecs.VAR_LONG.decode(b);
            String t = ByteBufCodecs.stringUtf8(256).decode(b);
            return new HudArg(k, v, t);
        }

        @Override
        public void encode(ByteBuf b, HudArg a) {
            b.writeByte(a.kind().ordinal());
            ByteBufCodecs.VAR_LONG.encode(b, a.value());
            ByteBufCodecs.stringUtf8(256).encode(b, a.text());
        }
    };

    public static final StreamCodec<ByteBuf, HudLinePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HudLinePayload decode(ByteBuf b) {
            byte kind = b.readByte(), pri = b.readByte();
            String key = ByteBufCodecs.stringUtf8(128).decode(b);
            int n = b.readByte();
            List<HudArg> args = new ArrayList<>(n);
            for (int i = 0; i < n; i++) args.add(ARG.decode(b));
            return new HudLinePayload(kind, pri, key, args, b.readBoolean());
        }

        @Override
        public void encode(ByteBuf b, HudLinePayload p) {
            b.writeByte(p.kind).writeByte(p.priority);
            ByteBufCodecs.stringUtf8(128).encode(b, p.key);
            b.writeByte(p.args.size());
            for (HudArg a : p.args) ARG.encode(b, a);
            b.writeBoolean(p.clear);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
