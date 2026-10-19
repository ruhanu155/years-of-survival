package com.yearofice.sim.fire;

/**
 * The Fire skill's rep count (Crafting and building in depth › Practice): a fire lit by friction counts 1, a failed
 * attempt 0.5, and a skill gains at most N ÷ 5 reps a game day (at least 2), so Practiced takes at least 5 days.
 * Practice never decays.
 */
public final class FirePractice {
    private FirePractice() {}

    /** Reps a skill with {@code n} reps to Practiced may gain in one game day. */
    public static double dailyCap(int n) { return Math.max(2, n / 5.0); }

    /**
     * The reps actually gained: {@code add}, cut so today's total never passes the daily cap.
     *
     * @param gainedToday reps already gained this game day
     */
    public static double gain(double gainedToday, double add, int n) {
        return Math.max(0, Math.min(add, dailyCap(n) - gainedToday));
    }
}
