package com.yearofice.sim.livestock;

/**
 * The {@code [livestock]} dials (Raising animals › Being away): need decay, predator rolls, escapes and growth, each a
 * multiplier on the blueprint's Standard numbers. Gentle needs ×0.7 and predators ×0.5; Brutal ×1.3 and ×1.5.
 */
public record LivestockTuning(double needDecayScale, double predatorScale, double escapeScale, double growthScale) {
    public enum Preset {
        GENTLE(new LivestockTuning(0.7, 0.5, 0.5, 1.15)),
        STANDARD(new LivestockTuning(1.0, 1.0, 1.0, 1.0)),
        BRUTAL(new LivestockTuning(1.3, 1.5, 1.5, 0.9));

        public final LivestockTuning tuning;

        Preset(LivestockTuning t) { tuning = t; }
    }
}
