package com.yearofice.sim.crop;

/** The {@code [garden]} dials except the frost offset (which can be negative and is read separately). Standard is the blueprint's numbers. */
public record GardenTuning(double germinationScale, double yieldScale, double pestScale, double wornBedLoss, double workTimeScale) {
    public enum Preset {
        GENTLE(new GardenTuning(1.15, 1.2, 0.5, 0.05, 0.75), 1.0),
        STANDARD(new GardenTuning(1.0, 1.0, 1.0, 0.10, 1.0), 0.0),
        BRUTAL(new GardenTuning(0.9, 0.85, 1.25, 0.10, 1.0), -1.0);

        public final GardenTuning tuning;
        public final double frostOffsetC;

        Preset(GardenTuning t, double frost) { tuning = t; frostOffsetC = frost; }
    }
}
