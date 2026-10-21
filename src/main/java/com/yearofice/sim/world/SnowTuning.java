package com.yearofice.sim.world;

/** Every number of the snow pack ({@code [snow]}): fall, settling, melt, crust and the four snow classes. */
public record SnowTuning(
        double snowBelowC, double freshDensity, double maxDensity, double settlePerDay, double windPackMs,
        double windPackDensity, double meltMmPerDegDay, double wetAboveC, double crustBelowC, double crustBuryCm,
        double underSpruceX, double southMeltX, int southFromDoy, double leeX, double leeWindMs,
        double layerCm, int maxLayers) {}
