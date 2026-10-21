package com.yearofice.sim.hunt;

/** Gentle, Standard and Brutal defaults for {@code [hunt]}. Standard is the Blueprint; GameTests run on it. Gentle forgives the draw sway and blood fades slower; Brutal the reverse. */
public enum HuntPreset {
    GENTLE(make(0.7, 1.25, 0.8)),
    STANDARD(make(1.0, 1.0, 1.0)),
    BRUTAL(make(1.3, 0.8, 1.25));

    public final HuntTuning tuning;

    HuntPreset(HuntTuning t) { this.tuning = t; }

    /** Only sway, the blood's life and the bear's bluff share move with the preset. */
    private static HuntTuning make(double swayX, double bloodX, double bluffX) {
        return new HuntTuning(9.81, 0.08, 4.0 * swayX, 0.02, 0.04, 0.6, 1.5, 120 * bloodX, 0.5, 2.0, 120, 0.5, 0.4, 0.0, 0.5,
                0.3, 0.7, 40, Math.min(0.95, 0.7 * bluffX));
    }
}
