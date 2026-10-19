package com.yearofice.hunt.fishing.net;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fishing.FightModel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the fight is over (Minecraft development › Minigames in Minecraft terms: {@code fish_result}).
 * The server's outcome, the species and the landed weight in grams (0 unless landed); the client closes the overlay.
 */
public record FishResultPayload(int fightId, FightModel.Outcome outcome, String species, int grams) implements CustomPacketPayload {
    public static final Type<FishResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fish_result"));
    public static final StreamCodec<ByteBuf, FishResultPayload> CODEC = StreamCodec.of(FishResultPayload::write, FishResultPayload::read);

    private static void write(ByteBuf buf, FishResultPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.fightId);
        buf.writeByte(p.outcome.ordinal());
        ByteBufCodecs.stringUtf8(64).encode(buf, p.species);
        ByteBufCodecs.VAR_INT.encode(buf, p.grams);
    }

    private static FishResultPayload read(ByteBuf buf) {
        int id = ByteBufCodecs.VAR_INT.decode(buf);
        FightModel.Outcome o = FightModel.Outcome.values()[Math.floorMod(buf.readByte(), FightModel.Outcome.values().length)];
        return new FishResultPayload(id, o, ByteBufCodecs.stringUtf8(64).decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
