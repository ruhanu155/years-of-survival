package com.yearofice.sim.companion;

import java.util.random.RandomGenerator;

/**
 * The February leave roll (Minecraft development › Taming, livestock and raised young): below 80 trust a tamed wolf
 * may go to join a pack, p = (80 - trust) / 400 a game day, halved after a feed and a night under a roof. Gentle
 * never; Standard holds it as a named wild record and it walks back 10-20 days later; Brutal for good.
 */
public final class WolfLeaving {
    private WolfLeaving() {}

    public static final int FEBRUARY_DAYS = 28;

    public static double dailyP(CompanionTuning t, double trust, boolean fedAndRoofed) {
        if (t.wolfLeaves() == CompanionTuning.NEVER) return 0;
        double p = Math.max(0, (t.leaveBase() - trust) / t.leaveDivisor());
        return fedAndRoofed ? p / 2 : p;
    }

    /** {@code leaveDay} 1-28 of February (0 = stayed); {@code returnOffsetDays} days after leaving (0 = never back). */
    public record Outcome(int leaveDay, int returnOffsetDays) {
        public boolean left() { return leaveDay > 0; }

        public boolean returns() { return returnOffsetDays > 0; }
    }

    public static int returnOffset(CompanionTuning t, RandomGenerator rng) {
        if (t.wolfLeaves() != CompanionTuning.RECOVERABLE) return 0;
        return t.returnMinDays() + rng.nextInt(t.returnMaxDays() - t.returnMinDays() + 1);
    }

    /** One whole February at a steady trust. */
    public static Outcome february(CompanionTuning t, double trust, boolean fedAndRoofed, RandomGenerator rng) {
        double p = dailyP(t, trust, fedAndRoofed);
        for (int d = 1; d <= FEBRUARY_DAYS; d++) {
            if (rng.nextDouble() < p) return new Outcome(d, returnOffset(t, rng));
        }
        return new Outcome(0, 0);
    }

    public record Stats(int februaries, int lost, int returned) {
        public double lostPct() { return 100.0 * lost / februaries; }

        public double returnedOfLostPct() { return lost == 0 ? 100 : 100.0 * returned / lost; }
    }

    /** {@code /yoi wolf leave test}: many Februaries at one trust. */
    public static Stats simulate(CompanionTuning t, double trust, boolean fedAndRoofed, int n, long seed) {
        java.util.SplittableRandom rng = new java.util.SplittableRandom(seed);
        int lost = 0, back = 0;
        for (int i = 0; i < n; i++) {
            Outcome o = february(t, trust, fedAndRoofed, rng);
            if (o.left()) lost++;
            if (o.returns()) back++;
        }
        return new Stats(n, lost, back);
    }
}
