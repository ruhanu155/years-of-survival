package com.yearofice.sim.plant;

/**
 * The {@code [forage]} dials (Plants and fungi › Debug commands and tuning). Standard is the plant files' numbers.
 *
 * @param yieldScale         x units per clump
 * @param workTimeScale      x game minutes per action
 * @param regrowScale        x regrow and refruit days
 * @param strippedThreshold  picked share that halves next year
 * @param poisonScale        x Condition lost to plant poisons
 * @param windowJitterDays   most a year can shift a ripeness window
 * @param sapScale           x litres a tap a day
 * @param spoilScale         x spoil_days_at_5c of plant food
 * @param lickRegrowKgPerWeek kg of crust a mineral lick regrows a week (Open world in depth › Salt from the mineral lick)
 * @param patchDensity       share of 24 m cells that hold a plant patch
 */
public record ForageTuning(double yieldScale, double workTimeScale, double regrowScale, double strippedThreshold,
                           double poisonScale, double windowJitterDays, double sapScale, double spoilScale,
                           double lickRegrowKgPerWeek, double patchDensity) {
    public enum Preset {
        GENTLE(new ForageTuning(1.3, 0.75, 0.75, 0.85, 0.5, 10, 1.2, 1.25, 0.2, 0.45)),
        STANDARD(new ForageTuning(1.0, 1.0, 1.0, 0.7, 1.0, 10, 1.0, 1.0, 0.2, 0.35)),
        BRUTAL(new ForageTuning(0.8, 1.0, 1.25, 0.6, 1.0, 14, 0.9, 0.85, 0.2, 0.25));

        public final ForageTuning tuning;

        Preset(ForageTuning t) { tuning = t; }
    }

    public static ForageTuning standard() { return Preset.STANDARD.tuning; }
}
