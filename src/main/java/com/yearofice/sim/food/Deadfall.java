package com.yearofice.sim.food;

import com.yearofice.sim.decay.Thresholds;

/**
 * The figure-4 and mouse deadfalls' rolls (Blueprint › Traps: the build table, Catch chance, Wear). A roll asks every
 * animal of the target species in range: P = 1 − (1 − p_s × m)^n. A tripped rock kills the right size 90% of the time
 * and anything heavier than a tenth of the rock 50%; at practice 0 the rock slips 15% of sets (a cut finger and 2
 * Condition). The sticks last 10 triggers; the rock never wears out. Pure Java; original Year of Ice code.
 */
public final class Deadfall {
    private Deadfall() {}

    public enum Outcome { EMPTY, KILL, MISS, SLIP }

    /**
     * @param pS          per animal per roll (marten 0.06; a mouse 0.30 on a winter night)
     * @param killRight   kill chance when the rock fits the animal (0.9)
     * @param killHeavy   kill chance when the animal is heavier than a tenth of the rock (0.5)
     * @param slipP       the rock slips while setting at practice 0 (0.15)
     * @param sticksLife  triggers the sticks last (10)
     */
    public record Tuning(double pS, double killRight, double killHeavy, double slipP, int sticksLife) {}

    public static double pCatch(double pS, double mult, double n) {
        double p = Math.min(1, Math.max(0, pS * mult));
        return n <= 0 ? 0 : 1 - Math.pow(1 - p, n);
    }

    /** Does the rock slip while the set is made? Keyed by the place and set number, so a reload never rerolls it. */
    public static boolean slips(long seed, int x, int y, int z, long setNo, double slipP, double practice) {
        double p = slipP * Math.max(0, 1 - practice);
        return Thresholds.stream(seed, "deadfall_slip", x, y, z, setNo).nextDouble() < p;
    }

    /** One dawn or dusk roll: nothing came, the rock fell and killed, or the animal pulled the bait and got away. */
    public static Outcome roll(long seed, int x, int y, int z, long roll, double n, double mult, boolean heavy, Tuning t) {
        var s = Thresholds.stream(seed, "deadfall", x, y, z, roll);
        double came = s.nextDouble(), kill = s.nextDouble();
        if (came >= pCatch(t.pS(), mult, n)) return Outcome.EMPTY;
        return kill < (heavy ? t.killHeavy() : t.killRight()) ? Outcome.KILL : Outcome.MISS;
    }
}
