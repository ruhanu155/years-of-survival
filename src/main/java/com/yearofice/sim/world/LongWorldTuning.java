package com.yearofice.sim.world;

/**
 * The {@code [longworld]} dials: the world's slow answers to years of play. {@code wildfireYearPct} is how many years
 * are drawn as wildfire years; {@code escalateWildfirePct} and {@code escalateExtremePct} the chance a lightning
 * strike that lights a tree becomes a wildfire in one or the other; {@code regrowthTimeScale} stretches the regrowth
 * calendar and {@code decayRateScale} multiplies every rot rate of an aged camp.
 */
public record LongWorldTuning(double wildfireYearPct, double escalateWildfirePct, double escalateExtremePct,
                              double regrowthTimeScale, double decayRateScale) {
    public enum Preset {
        GENTLE(new LongWorldTuning(10, 20, 40, 0.85, 0.8)),
        STANDARD(new LongWorldTuning(20, 30, 60, 1.0, 1.0)),
        BRUTAL(new LongWorldTuning(30, 40, 80, 1.2, 1.25));

        public final LongWorldTuning tuning;

        Preset(LongWorldTuning t) { tuning = t; }
    }
}
