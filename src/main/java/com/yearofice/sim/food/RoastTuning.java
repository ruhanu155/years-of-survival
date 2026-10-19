package com.yearofice.sim.food;

/**
 * Every cooking and eating number of the slice, as the {@code [cooking]} section of {@code yearofice-common.toml}
 * holds them (Blueprint › Cooking; Items › Food values). Standard is the Blueprint's numbers; the docs give no
 * preset differences for cooking, so Gentle and Brutal carry the same values until a tuning round sets them.
 *
 * @param spitMinPerKg      spit or stick roast: game minutes per kg of meat (30)
 * @param spitWholeHareMin  a whole hare on the spit: game minutes (25)
 * @param wholeHareG        grams of dressed meat that count as one whole hare (900: the slice's dressing gives 9
 *                          units of 100 g; Bestiary › Dressing by size class)
 * @param spitFishMin       a whole fish under {@code spitFishMaxG} on the spit: game minutes (12)
 * @param spitFishMaxG      the "under 1 kg" line for the whole-fish time; heavier fish roast at the per-kg rate
 * @param spitCapacityG     the most a spit holds at once (one whole hare's meat)
 * @param speedLow          cooking speed on a Low fire, relative to the cook time
 * @param speedSteady       on a Steady fire
 * @param speedHot          on a Hot fire
 * @param speedCoals        over coals (the table's own station: "spit over coals")
 * @param eatTicks          ticks one bite of food takes to eat (one 100 g unit or one whole piece)
 */
public record RoastTuning(double spitMinPerKg, double spitWholeHareMin, double wholeHareG, double spitFishMin,
                          double spitFishMaxG, double spitCapacityG, double speedLow, double speedSteady,
                          double speedHot, double speedCoals, int eatTicks) {

    public RoastTuning {
        if (!(spitMinPerKg > 0) || !(spitWholeHareMin > 0) || !(wholeHareG > 0) || !(spitFishMin > 0)
                || !(spitFishMaxG > 0) || !(spitCapacityG > 0)) {
            throw new IllegalArgumentException("cook times and weights must be positive");
        }
        if (speedLow < 0 || speedSteady < 0 || speedHot < 0 || speedCoals < 0) {
            throw new IllegalArgumentException("speeds must be non-negative");
        }
        if (eatTicks < 1) throw new IllegalArgumentException("eating takes at least a tick");
    }

    public static RoastTuning standard() { return Preset.STANDARD.tuning; }

    public enum Preset {
        GENTLE(new RoastTuning(30, 25, 900, 12, 1000, 900, 1.0, 1.0, 1.0, 1.0, 32)),
        STANDARD(new RoastTuning(30, 25, 900, 12, 1000, 900, 1.0, 1.0, 1.0, 1.0, 32)),
        BRUTAL(new RoastTuning(30, 25, 900, 12, 1000, 900, 1.0, 1.0, 1.0, 1.0, 32));

        public final RoastTuning tuning;

        Preset(RoastTuning t) { this.tuning = t; }
    }
}
