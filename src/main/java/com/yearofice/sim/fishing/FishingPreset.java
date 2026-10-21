package com.yearofice.sim.fishing;

/**
 * Gentle, Standard and Brutal defaults for the {@code [fishing]} section (Blueprint › Realism presets). Standard is
 * the Blueprint's numbers; GameTests run on it. The presets move only what the Blueprint ties to a preset: stock
 * capacity follows Animal numbers (×1.5, ×1.0, ×0.7) and the fishing assist is on in Gentle (The fight, as built ›
 * Assist). Every other key is the same in all three.
 */
public enum FishingPreset {
    GENTLE(make(1.5, 1)),
    STANDARD(make(1.0, 0)),
    BRUTAL(make(0.7, 0));

    public final FishingTuning tuning;

    FishingPreset(FishingTuning tuning) { this.tuning = tuning; }

    private static FishingTuning make(double stockX, int assist) {
        return new FishingTuning(0.84, 1.5, 2, 5, 30, 5, stockX, 1.0, 2, 30, 15, 1.5, 10, 5, 0.25, 60, 10, 0.5,
                1.0, 2.5, 1.0, 3, 25, 10, 60, assist, 2, 6, 0.0, 1, 0.08);
    }
}
