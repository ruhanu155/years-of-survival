package com.yearofice.hunt.fishing.net;

import com.yearofice.core.YearOfIce;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the fight's inputs, one byte a tick ({@code FightModel.Input.toByte}), batched every 4 ticks
 * (Minecraft development › Minigames in Minecraft terms). {@code fightId} {@value #STRIKE} with no inputs is a
 * left-mouse click on the bobber before a fight: the strike, or a twitch of the bait.
 */
public record FishInputPayload(int fightId, int firstTick, byte[] inputs) implements CustomPacketPayload {
    public static final int STRIKE = -1;
    public static final int MAX_INPUTS = 64;
    public static final Type<FishInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fish_input"));
    public static final StreamCodec<ByteBuf, FishInputPayload> CODEC = StreamCodec.of(FishInputPayload::write, FishInputPayload::read);

    public static FishInputPayload strike() { return new FishInputPayload(STRIKE, 0, new byte[0]); }

    private static void write(ByteBuf buf, FishInputPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.fightId);
        ByteBufCodecs.VAR_INT.encode(buf, p.firstTick);
        int n = Math.min(MAX_INPUTS, p.inputs.length);
        ByteBufCodecs.VAR_INT.encode(buf, n);
        buf.writeBytes(p.inputs, 0, n);
    }

    private static FishInputPayload read(ByteBuf buf) {
        int id = ByteBufCodecs.VAR_INT.decode(buf);
        int first = ByteBufCodecs.VAR_INT.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        if (n < 0 || n > MAX_INPUTS) throw new IllegalArgumentException("fish_input: " + n + " inputs");
        byte[] in = new byte[n];
        buf.readBytes(in);
        return new FishInputPayload(id, first, in);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
