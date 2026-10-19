package com.yearofice.fire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.fire.Wood;

/**
 * One item's entry in the data map {@code yearofice:fuel} ({@code data/yearofice/data_maps/item/fuel.json}): which
 * wood it is, what share of a 15 kg armload one item makes, and its moisture when the stack carries no
 * {@code yearofice:moisture} component (RECONCILE 127: dryness is the component plus this map).
 */
public record FuelData(String wood, double armloadFraction, int moisturePct) {
    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("wood").forGetter(FuelData::wood),
            Codec.doubleRange(0.01, 4).fieldOf("armload_fraction").forGetter(FuelData::armloadFraction),
            Codec.intRange(0, 100).optionalFieldOf("moisture_pct", 18).forGetter(FuelData::moisturePct)
    ).apply(i, FuelData::new));

    public Wood woodType() { return Wood.byId(wood); }
}
