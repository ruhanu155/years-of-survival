package com.yearofice.hunt.fishing.net;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.FishingSpecies;
import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.Tackle;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Server to client: the fight's state, 5 times a second (Minecraft development › Minigames in Minecraft terms). The
 * first one of a fight also carries the fight's fixed inputs ({@link FightModel.Params}) so the client can run the
 * same lockstep model; later ones carry the server's state at its tick, which the client snaps to if it drifted.
 */
public record FishStatePayload(int fightId, @Nullable FightModel.Params open, FightModel.State state) implements CustomPacketPayload {
    public static final Type<FishStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fish_state"));
    public static final StreamCodec<ByteBuf, FishStatePayload> CODEC = StreamCodec.of(FishStatePayload::write, FishStatePayload::read);

    private static void write(ByteBuf buf, FishStatePayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.fightId);
        buf.writeBoolean(p.open != null);
        if (p.open != null) writeParams(buf, p.open);
        writeState(buf, p.state);
    }

    private static FishStatePayload read(ByteBuf buf) {
        int id = ByteBufCodecs.VAR_INT.decode(buf);
        FightModel.Params params = buf.readBoolean() ? readParams(buf) : null;
        return new FishStatePayload(id, params, readState(buf));
    }

    static void writeParams(ByteBuf buf, FightModel.Params p) {
        ByteBufCodecs.stringUtf8(64).encode(buf, p.species().id());
        buf.writeDouble(p.kg());
        Tackle t = p.tackle();
        for (double d : new double[] {t.lineRatedKg(), t.wetFactor(), t.conditionPct(), t.lineCarriedM(), t.flex(),
                t.retrieveSpeedMps(), t.retrievePullKg(), t.dragShare()}) buf.writeDouble(d);
        for (boolean b : new boolean[] {t.wet(), t.drag(), t.handLine(), t.gorge(), t.wireLeader()}) buf.writeBoolean(b);
        buf.writeLong(p.seed());
        buf.writeLong(p.fightId());
        buf.writeDouble(p.castM());
        buf.writeDouble(p.waterTempC());
        for (boolean b : new boolean[] {p.playerWeak(), p.coldHands(), p.assist(), p.snagsAtSpot(), p.brittleRod(), p.landingNet()}) buf.writeBoolean(b);
    }

    static FightModel.Params readParams(ByteBuf buf) {
        String species = ByteBufCodecs.stringUtf8(64).decode(buf);
        double kg = buf.readDouble();
        double[] d = new double[8];
        for (int i = 0; i < d.length; i++) d[i] = buf.readDouble();
        boolean[] b = new boolean[5];
        for (int i = 0; i < b.length; i++) b[i] = buf.readBoolean();
        Tackle t = new Tackle(d[0], d[1], b[0], d[2], d[3], d[4], d[5], d[6], b[1], d[7], b[2], b[3], b[4]);
        long seed = buf.readLong();
        long fightId = buf.readLong();
        double castM = buf.readDouble();
        double water = buf.readDouble();
        boolean[] f = new boolean[6];
        for (int i = 0; i < f.length; i++) f[i] = buf.readBoolean();
        return new FightModel.Params(FishingSpecies.byId(species), kg, t, seed, fightId, castM, f[0], f[1], f[2], water, f[3], f[4], f[5]);
    }

    static void writeState(ByteBuf buf, FightModel.State s) {
        buf.writeInt(s.tick());
        buf.writeByte(s.phase().ordinal());
        buf.writeByte(s.outcome().ordinal());
        for (double d : new double[] {s.stamina(), s.lineOutM(), s.tensionKg(), s.phaseLeftS(), s.pendingRunS(), s.nextRunS(),
                s.nextSpecialS(), s.spikeLeftS(), s.spikeX(), s.slackS(), s.tensionOnS(), s.calmS(), s.diveGiveS(), s.redS()}) {
            buf.writeDouble(d);
        }
        buf.writeBoolean(s.biteThroughArmed());
        buf.writeBoolean(s.specialSeen());
        buf.writeBoolean(s.reachedRed());
    }

    static FightModel.State readState(ByteBuf buf) {
        int tick = buf.readInt();
        FightModel.Phase phase = FightModel.Phase.values()[Math.floorMod(buf.readByte(), FightModel.Phase.values().length)];
        FightModel.Outcome outcome = FightModel.Outcome.values()[Math.floorMod(buf.readByte(), FightModel.Outcome.values().length)];
        double[] d = new double[14];
        for (int i = 0; i < d.length; i++) d[i] = buf.readDouble();
        boolean armed = buf.readBoolean(), seen = buf.readBoolean(), red = buf.readBoolean();
        return new FightModel.State(tick, phase, outcome, d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9], d[10],
                d[11], d[12], armed, seen, red, d[13]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
