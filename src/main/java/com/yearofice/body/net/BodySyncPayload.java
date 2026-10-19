package com.yearofice.body.net;

import com.yearofice.core.YearOfIce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:body_sync}: the owner's own vitals, for the vitals strip, the corner screen and the messenger
 * pages (Minecraft development › Layer 7, Sync). Sent when a shown value moves over 1% of its range, at least once
 * a game hour, and at once after eating, drinking, an injury or an SOS. It also carries the slice's weather
 * readout, until {@code env_sync} exists.
 *
 * @param feel       the feels-like terms: air, wind chill, wet penalty, clothing, shelter, fire, activity, body
 * @param precip     {@link com.yearofice.sim.env.Precip} ordinal
 */
public record BodySyncPayload(long minute, float kg, float bmi, float kcalInToday, float kcalOutToday, float balance24h,
                              float energy, float energyCap, float warmth, float feelsLikeC, float[] feel,
                              float wetness, float water, float stomachPct, float condition, float strength,
                              float macroCarbs, float macroFat, float macroProtein, float drunkMlToday, boolean gutIll,
                              int sos, int activity, float airC, int precip, float precipMmH, float windMs,
                              boolean roofed, int battery) implements CustomPacketPayload {

    public static final Type<BodySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "body_sync"));

    public static final int FEEL_TERMS = 8;

    public static final StreamCodec<RegistryFriendlyByteBuf, BodySyncPayload> STREAM_CODEC =
            StreamCodec.of(BodySyncPayload::write, BodySyncPayload::read);

    static void write(RegistryFriendlyByteBuf b, BodySyncPayload p) {
        b.writeVarLong(p.minute);
        b.writeFloat(p.kg);
        b.writeFloat(p.bmi);
        b.writeFloat(p.kcalInToday);
        b.writeFloat(p.kcalOutToday);
        b.writeFloat(p.balance24h);
        b.writeFloat(p.energy);
        b.writeFloat(p.energyCap);
        b.writeFloat(p.warmth);
        b.writeFloat(p.feelsLikeC);
        for (int i = 0; i < FEEL_TERMS; i++) b.writeFloat(i < p.feel.length ? p.feel[i] : 0);
        b.writeFloat(p.wetness);
        b.writeFloat(p.water);
        b.writeFloat(p.stomachPct);
        b.writeFloat(p.condition);
        b.writeFloat(p.strength);
        b.writeFloat(p.macroCarbs);
        b.writeFloat(p.macroFat);
        b.writeFloat(p.macroProtein);
        b.writeFloat(p.drunkMlToday);
        b.writeBoolean(p.gutIll);
        b.writeVarInt(p.sos);
        b.writeVarInt(p.activity);
        b.writeFloat(p.airC);
        b.writeVarInt(p.precip);
        b.writeFloat(p.precipMmH);
        b.writeFloat(p.windMs);
        b.writeBoolean(p.roofed);
        b.writeVarInt(p.battery);
    }

    static BodySyncPayload read(RegistryFriendlyByteBuf b) {
        long minute = b.readVarLong();
        float kg = b.readFloat(), bmi = b.readFloat(), in = b.readFloat(), out = b.readFloat(), bal = b.readFloat();
        float energy = b.readFloat(), cap = b.readFloat(), warmth = b.readFloat(), feels = b.readFloat();
        float[] feel = new float[FEEL_TERMS];
        for (int i = 0; i < FEEL_TERMS; i++) feel[i] = b.readFloat();
        float wet = b.readFloat(), water = b.readFloat(), stomach = b.readFloat(), cond = b.readFloat();
        float strength = b.readFloat(), carbs = b.readFloat(), fat = b.readFloat(), protein = b.readFloat();
        float drunk = b.readFloat();
        boolean gut = b.readBoolean();
        int sos = b.readVarInt(), activity = b.readVarInt();
        float air = b.readFloat();
        int precip = b.readVarInt();
        float mmH = b.readFloat(), wind = b.readFloat();
        boolean roofed = b.readBoolean();
        int battery = b.readVarInt();
        return new BodySyncPayload(minute, kg, bmi, in, out, bal, energy, cap, warmth, feels, feel, wet, water, stomach,
                cond, strength, carbs, fat, protein, drunk, gut, sos, activity, air, precip, mmH, wind, roofed, battery);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** True when any shown value moved more than 1% of its range since {@code last}. */
    public boolean differs(BodySyncPayload last) {
        if (last == null) return true;
        return d(kg, last.kg, 1) || d(warmth, last.warmth, 100) || d(water, last.water, 100)
                || d(energy, last.energy, 10) || d(energyCap, last.energyCap, 10) || d(wetness, last.wetness, 1)
                || d(stomachPct, last.stomachPct, 1) || d(condition, last.condition, 100)
                || d(feelsLikeC, last.feelsLikeC, 100) || d(kcalInToday, last.kcalInToday, 3000)
                || d(kcalOutToday, last.kcalOutToday, 3000) || d(airC, last.airC, 100)
                || sos != last.sos || gutIll != last.gutIll || precip != last.precip || roofed != last.roofed
                || battery / 100 != last.battery / 100;
    }

    private static boolean d(float a, float b, float range) { return Math.abs(a - b) > 0.01f * range; }
}
