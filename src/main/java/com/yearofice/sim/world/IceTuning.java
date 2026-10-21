package com.yearofice.sim.world;

/**
 * Every number of the ice model ({@code [ice]} in yearofice-common.toml; Gentle, Standard, Brutal in
 * {@link SeasonPreset}). Growth is Stefan's law per class: dh/dFDD = a^2 / (2h), slowed by snow on the ice.
 */
public record IceTuning(
        double onsetFddRiver, double onsetFddLake, double onsetFddPond, double onsetFddSea,
        double growthRiver, double growthLake, double growthPond, double growthSea,
        double warmDecay, double snowInsulCm, double meltCmPerPdd, double rainMeltCmPerMm,
        double rottenLossPct, double rottenStrength, double thinCm, double sledCm, double skimCm,
        double breakupCmRiver, double floodPulseM, double floodPerSnowCm, double floodKeepPerDay, double floodMinM,
        double holeSkinMinAtM20, double holeSkinAboveC, double holeSkinExp) {

    public double onset(IceModel.BodyClass c) {
        return switch (c) { case RIVER -> onsetFddRiver; case LAKE -> onsetFddLake; case POND -> onsetFddPond; case SEA_EDGE -> onsetFddSea; };
    }

    public double growth(IceModel.BodyClass c) {
        return switch (c) { case RIVER -> growthRiver; case LAKE -> growthLake; case POND -> growthPond; case SEA_EDGE -> growthSea; };
    }
}
