package com.yearofice.sim.fishing;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The numbers a fish species file ({@code data/yearofice/fish/<id>.json}) gives the bite, the size draw and the
 * fight (Blueprint › Fishing in depth › Fish species; Waters and fish › Fight data). No species is hard-coded in
 * the cores: every rule reads this record.
 *
 * @param bitesPerMin    b, the base bite chance per game minute
 * @param runEveryS      seconds between runs at full stamina, [min, max]
 * @param runLengthS     run length, [min, max]
 * @param tirePerS       1 ÷ "Tires in (s at full pressure)"
 * @param special        the species' special move
 * @param specialEveryS  seconds between head-shakes or twists, [min, max]; ignored for dives and none
 * @param specialSpike   the spike multiplier of a head-shake (1.4) or twist (1.2)
 * @param nightFactor    m_time at night: burbot 2.0, grayling 0.5, others 0.8
 * @param trophyKg       the trophy band [min, max] at spots with q ≥ 0.7, or null
 * @param bait           m_bait by bait key; any bait not listed counts 0.2
 */
public record FishSpecies(String id, double kgMin, double kgMax, double bitesPerMin, double kcalPerKg,
                          double runPullX, double[] runEveryS, double[] runLengthS, double jumpChance,
                          Special special, double[] specialEveryS, double specialSpike, double tirePerS,
                          double biteThroughNoLeader, double nightFactor, double[] trophyKg,
                          Map<String, Double> bait, boolean surfaceFeeder) {
    public static final double WRONG_BAIT = 0.2;

    public enum Special { NONE, HEAD_SHAKE, TWIST, DIVE, LONG_RUN, BORE }

    public FishSpecies {
        if (kgMax < kgMin || kgMin <= 0) throw new IllegalArgumentException(id + ": bad kg range");
        bait = Collections.unmodifiableMap(new TreeMap<>(bait));
        runEveryS = runEveryS.clone();
        runLengthS = runLengthS.clone();
        specialEveryS = specialEveryS == null ? new double[] {0, 0} : specialEveryS.clone();
        trophyKg = trophyKg == null ? null : trophyKg.clone();
    }

    public double baitMultiplier(String baitKey) {
        Double m = bait.get(baitKey);
        return m == null ? WRONG_BAIT : m;
    }

    // Reference species from Blueprint › Fishing in depth, used by tests and as the shape of the JSON.

    public static FishSpecies northernPike() {
        return new FishSpecies("northern_pike", 1, 10, 0.015, 1500, 0.6, new double[] {4, 9}, new double[] {1.5, 3},
                0.05, Special.HEAD_SHAKE, new double[] {2, 5}, 1.4, 1.0 / 25, 0.5, 0.8, new double[] {10, 15},
                Map.of("live_minnow", 2.0, "spoon", 1.2, "fish_chunk", 1.0, "live_chub", 2.0, "shell_spoon", 0.85), false);
    }

    public static FishSpecies arcticGrayling() {
        return new FishSpecies("arctic_grayling", 0.3, 1.2, 0.12, 800, 1.2, new double[] {2, 5}, new double[] {0.5, 1.5},
                0.30, Special.NONE, null, 1.0, 1.0 / 8, 0, 0.5, null,
                Map.of("fly", 1.2, "insect", 1.0, "worm", 0.6, "roe", 0.6, "caddis", 1.3, "stonefly", 1.4), true);
    }

    public static FishSpecies burbot() {
        return new FishSpecies("burbot", 1, 4, 0.03, 1600, 0.35, new double[] {10, 20}, new double[] {1, 2},
                0.0, Special.TWIST, new double[] {2, 2}, 1.2, 1.0 / 30, 0.1, 2.0, null,
                Map.of("fish_chunk", 1.0, "live_sculpin", 1.0, "guts", 0.6, "stonefly", 0.8), false);
    }

    public static FishSpecies lakeTrout() {
        return new FishSpecies("lake_trout", 2, 10, 0.01, 1600, 0.45, new double[] {8, 15}, new double[] {2, 5},
                0.0, Special.DIVE, null, 1.0, 1.0 / 40, 0.05, 0.8, null,
                Map.of("jig", 1.0, "fish_chunk", 1.0, "live_sculpin", 0.6), false);
    }

    public static FishSpecies humpbackWhitefish() {
        return new FishSpecies("humpback_whitefish", 0.5, 2, 0.03, 1500, 0.4, new double[] {8, 15}, new double[] {1, 2},
                0.0, Special.NONE, null, 1.0, 1.0 / 10, 0, 0.8, null,
                Map.of("insect", 1.0, "roe", 1.0, "worm", 0.6, "caddis", 1.0), true);
    }
}
