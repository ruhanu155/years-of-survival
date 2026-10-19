package com.yearofice.worldgen.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * The generator's options, saved with the dimension in {@code level.dat} (World creation options and presets).
 * M2 holds the cut: {@code valley} (the 8 km home valley, the default preset) or {@code slice} (the 1 km slice
 * valley, M2.5); and, for tests only, {@code layer_seed}, which builds the layers from a fixed seed instead of
 * the level's. Realism, mode and day length join with the Customize screen.
 */
public record GenSettings(String cut, Optional<Long> layerSeed) {
    public static final String VALLEY = "valley", SLICE = "slice";

    public static final Codec<GenSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("cut", VALLEY).forGetter(GenSettings::cut),
            Codec.LONG.optionalFieldOf("layer_seed").forGetter(GenSettings::layerSeed)).apply(i, GenSettings::new));

    public boolean slice() { return SLICE.equals(cut); }

    public long seedFor(long levelSeed) { return layerSeed.orElse(levelSeed); }
}
