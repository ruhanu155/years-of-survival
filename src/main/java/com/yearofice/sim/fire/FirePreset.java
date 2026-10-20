package com.yearofice.sim.fire;

/**
 * Gentle, Standard and Brutal defaults for the {@code [fire]} section (Blueprint › Realism presets). Standard is
 * the Blueprint's numbers; GameTests run on it. Gentle forgives weather and the first fires; Brutal shortens the
 * margins but never changes a rule.
 */
public enum FirePreset {
    GENTLE(new FireTuning(50, 3, 75, 6, 3, 25, 75, 480, 2, 15, 45, 30, 0.85, 12, 1.0,
            45, 25, 98, 70, 6, 80, 98, 8, 50, 3.0, 95, 4, 3, 6, 15,
            75, 225, 150, 95, 70, 1, 2, 4, 8, 20, 5, 0.25, 8)),
    STANDARD(new FireTuning(40, 3, 60, 6, 2, 20, 60, 360, 2, 10, 30, 20, 0.75, 10, 1.25,
            40, 20, 95, 50, 5, 70, 95, 10, 60, 4.0, 85, 3, 2, 6, 20,
            60, 180, 120, 90, 60, 2, 4, 8, 15, 20, 10, 0.25, 10)),
    BRUTAL(new FireTuning(32, 4, 45, 5, 2, 15, 45, 360, 2, 8, 25, 15, 0.7, 8, 1.5,
            35, 15, 90, 40, 4, 60, 90, 12, 70, 5.0, 75, 3, 2, 6, 25,
            48, 144, 96, 85, 50, 3, 6, 12, 20, 20, 15, 0.25, 12));

    public final FireTuning tuning;

    FirePreset(FireTuning tuning) { this.tuning = tuning; }
}
