package com.yearofice.sim.ecosystem;

import com.yearofice.core.hash.Mulberry32;

/**
 * Talk as you walk (Water, calls and encounters › B7; research round B). The voice carries 80 m over open ground, 50 m
 * in dense cover, half that beside rapids. A bear or moose that hears the player first turns Alert and moves off 85%
 * of the time (70% for a sow with cubs), and for it the "surprised inside 25 m" trigger cannot fire. Pure Java.
 */
public final class TalkWalk {
    private TalkWalk() {}

    public enum Outcome { MOVES_OFF, NORMAL_DECISION, SURPRISED_DEFENSIVE, UNAWARE_PASS }

    public static final double SURPRISE_M = 25;
    /** Without talk, an unaware animal met inside 25 m in thick cover is surprised this often (Danger in detail). */
    public static final double BASELINE_SURPRISE = 0.45;

    public record Tuning(double openM, double coverM, double avoid, double avoidSow) {
        public static Tuning standard() { return new Tuning(80, 50, 0.85, 0.70); }
    }

    public static double radius(boolean denseCover, boolean beside_rapids, Tuning t) {
        double r = denseCover ? t.coverM() : t.openM();
        return beside_rapids ? r / 2 : r;
    }

    /**
     * One walk toward an unaware animal in cover that ends inside {@code metresAtMeet}. With talk on and the animal
     * inside the heard radius, it avoids, else the normal decision runs without the surprise trigger. With talk off,
     * the baseline surprise applies inside 25 m.
     */
    public static Outcome approach(boolean talking, double metresAtMeet, double heardRadiusM, boolean sow, Tuning t, Mulberry32 r) {
        double u = r.nextDouble(), v = r.nextDouble();
        if (talking && metresAtMeet <= heardRadiusM) return u < (sow ? t.avoidSow() : t.avoid()) ? Outcome.MOVES_OFF : Outcome.NORMAL_DECISION;
        if (metresAtMeet <= SURPRISE_M) return v < BASELINE_SURPRISE ? Outcome.SURPRISED_DEFENSIVE : Outcome.UNAWARE_PASS;
        return Outcome.UNAWARE_PASS;
    }
}
