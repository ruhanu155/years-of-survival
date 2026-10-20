package com.yearofice.sim.food;

/**
 * The drying rack and smoke rack numbers (Blueprint › Preserving in detail; Structures and buildables › Drying rack
 * and smoke rack). Standard is the Blueprint's numbers; the docs give no preset differences, so Gentle and Brutal carry
 * the same values until a tuning round sets them.
 *
 * @param dryHours       dry game hours a batch of 1 cm strips needs (2 dry days: 48)
 * @param rainPenaltyH   hours an uncovered rain hour adds to the batch (6)
 * @param rainResetH     consecutive uncovered rain hours (a full day) that restart the batch (24)
 * @param smokeMinH      shortest hot-smoke run, game hours (8)
 * @param smokeMaxH      longest hot-smoke run, game hours (12)
 * @param smokeWindMs    wind above this (m/s) takes the heat and smoke off the rack (8)
 * @param dryCapacityKg  fresh strips a drying rack holds (15)
 * @param smokeCapacityKg meat or fish a smoke rack holds (10)
 */
public record RackTuning(double dryHours, double rainPenaltyH, double rainResetH, double smokeMinH, double smokeMaxH,
                         double smokeWindMs, double dryCapacityKg, double smokeCapacityKg) {

    public RackTuning {
        if (!(dryHours > 0) || !(rainPenaltyH >= 0) || !(rainResetH >= 1) || !(smokeMinH > 0) || smokeMaxH < smokeMinH
                || !(smokeWindMs > 0) || !(dryCapacityKg > 0) || !(smokeCapacityKg > 0)) {
            throw new IllegalArgumentException("rack numbers out of range");
        }
    }

    public static RackTuning standard() { return Preset.STANDARD.tuning; }

    public enum Preset {
        GENTLE(new RackTuning(48, 6, 24, 8, 12, 8, 15, 10)),
        STANDARD(new RackTuning(48, 6, 24, 8, 12, 8, 15, 10)),
        BRUTAL(new RackTuning(48, 6, 24, 8, 12, 8, 15, 10));

        public final RackTuning tuning;

        Preset(RackTuning t) { this.tuning = t; }
    }
}
