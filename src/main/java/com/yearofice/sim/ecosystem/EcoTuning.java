package com.yearofice.sim.ecosystem;

/**
 * Every ecosystem number of the slice, as the {@code [ecosystem]} section of {@code yearofice-common.toml} holds
 * them. Densities are per km² of good habitat at K, the slice's stand-in for Ecology's regional counts
 * (Blueprint › Populations: 25–40 hares a 2 km region in the middle years of the cycle, packed into the willow and
 * spruce edges).
 *
 * @param hareDensityPerKm2      hares per km² of edge habitat at K (40, the top of the 25–40 range, as the core
 *                               valley's K is: a trap's home-range circle of 150 m then covers about 2.8 hares, so a
 *                               trail snare checked each morning averages 2.7 hares in 10 days and lands in 2–5 on
 *                               93% of runs, Ready for development M2.5 test 2)
 * @param ptarmiganGroupsPerKm2  ptarmigan groups per km² of open shrub
 * @param mooseDensityPerKm2     moose per km² of riparian willow
 * @param activeRadiusM          bodies appear within this distance of a player (Active tier, 150 m)
 * @param demoteRadiusM          and leave beyond this (180 m; the 30 m hysteresis stops flicker)
 * @param activeCap              at most this many of our animals are bodies in one level (40)
 * @param workMinPerS            the Work time-lapse rate (5 game minutes a real second)
 * @param minWorkEnergy          hold-to-work stops below this many energy bolts (2)
 */
public record EcoTuning(double hareDensityPerKm2, double ptarmiganGroupsPerKm2, double mooseDensityPerKm2,
                        double activeRadiusM, double demoteRadiusM, int activeCap,
                        double workMinPerS, double minWorkEnergy) {

    public static EcoTuning standard() { return Preset.STANDARD.tuning; }

    public enum Preset {
        GENTLE(new EcoTuning(55, 12, 1.5, 128, 160, 40, 5, 1)),
        STANDARD(new EcoTuning(40, 8, 1.0, 128, 160, 40, 5, 2)),
        BRUTAL(new EcoTuning(28, 5, 0.6, 128, 160, 40, 5, 2));

        public final EcoTuning tuning;

        Preset(EcoTuning t) { this.tuning = t; }
    }
}
