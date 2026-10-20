package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.build.Gates;

/** The value of the {@code yearofice:gates} attachment: the gate mask (Start always open). v1, every field defaulted. */
public record GateData(int mask) {
    public static final GateData NEW = new GateData(Gates.NEW_WORLD);

    public static final Codec<GateData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("mask", Gates.NEW_WORLD).forGetter(GateData::mask)).apply(i, GateData::new));
}
