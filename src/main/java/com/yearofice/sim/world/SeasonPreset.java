package com.yearofice.sim.world;

/** Gentle, Standard and Brutal numbers for the seasons, ice and snow (Realism presets). */
public enum SeasonPreset {
    GENTLE(scale(base(), 0.85, 1.08, 0.85), snow(2.7), season()),
    STANDARD(base(), snow(3.0), season()),
    BRUTAL(scale(base(), 1.2, 0.93, 1.15), snow(3.4), season());

    public final IceTuning ice;
    public final SnowTuning snow;
    public final SeasonTuning season;

    SeasonPreset(IceTuning ice, SnowTuning snow, SeasonTuning season) {
        this.ice = ice;
        this.snow = snow;
        this.season = season;
    }

    private static IceTuning base() {
        return new IceTuning(125, 80, 45, 200, 1.9, 2.6, 2.8, 1.7, 1.0, 40, 0.9, 0.4, 14, 0.5, 10, 20, 0.5, 6,
                1.2, 0.01, 0.75, 0.05, 45, -5, 2.0);
    }

    private static IceTuning scale(IceTuning b, double onsetX, double growthX, double meltX) {
        return new IceTuning(b.onsetFddRiver() * onsetX, b.onsetFddLake() * onsetX, b.onsetFddPond() * onsetX, b.onsetFddSea() * onsetX,
                b.growthRiver() * growthX, b.growthLake() * growthX, b.growthPond() * growthX, b.growthSea() * growthX,
                b.warmDecay(), b.snowInsulCm(), b.meltCmPerPdd() * meltX, b.rainMeltCmPerMm() * meltX, b.rottenLossPct(),
                b.rottenStrength(), b.thinCm(), b.sledCm(), b.skimCm(), b.breakupCmRiver(), b.floodPulseM(), b.floodPerSnowCm(),
                b.floodKeepPerDay(), b.floodMinM(), b.holeSkinMinAtM20(), b.holeSkinAboveC(), b.holeSkinExp());
    }

    private static SnowTuning snow(double ddf) {
        return new SnowTuning(1.0, 67, 280, 0.01, 9, 8, ddf, -0.5, -4, 4, 0.6, 1.5, 304, 1.8, 7, 12.5, 24);
    }

    private static SeasonTuning season() {
        return new SeasonTuning(19, 14, 130, 20, 150, 10, 190, 355, 137, 157, 6, 256, 6, 8);
    }
}
