package com.yearofice.sim.build;

/**
 * Practice's rules (Crafting and building in depth › Practice): practice speeds a job up to 40% and never changes
 * its calories; a finished job is 1 rep and a failed attempt 0.5; a skill gains at most N ÷ 5 reps a game day (at
 * least 2), so Practiced takes at least 5 days of real use; practice never decays.
 */
public final class Practice {
    private Practice() {}

    public static final double JOB_REP = 1.0, FAIL_REP = 0.5, MAX_SPEEDUP = 0.4;

    /** The three words the work preview shows. */
    public enum Word {
        CLUMSY("Clumsy"), HANDY("Handy"), PRACTICED("Practiced");

        public final String text;

        Word(String text) { this.text = text; }
    }

    /** Reps a skill with {@code n} reps to Practiced may gain in one game day. */
    public static double dailyCap(int n) { return Math.max(2, n / 5.0); }

    /** The reps actually gained: {@code add}, cut so today's total never passes the daily cap. */
    public static double gain(double gainedToday, double add, int n) {
        return Math.max(0, Math.min(add, dailyCap(n) - gainedToday));
    }

    /** The time factor {@code 1 − 0.4 · min(reps, N) ÷ N}: 1 for a beginner, 0.6 once Practiced. */
    public static double timeFactor(double reps, int n) {
        if (n <= 0) return 1;
        return 1 - MAX_SPEEDUP * Math.min(Math.max(reps, 0), n) / n;
    }

    /** Clumsy under a third of N, Handy up to N, Practiced from N. */
    public static Word word(double reps, int n) {
        if (reps >= n) return Word.PRACTICED;
        return reps < n / 3.0 ? Word.CLUMSY : Word.HANDY;
    }

    public static boolean practiced(double reps, int n) { return reps >= n; }
}
