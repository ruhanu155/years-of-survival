package com.yearofice.sim.build;

/**
 * The job-time and calorie formulas (Crafting and building in depth › Practice):
 * <pre>
 *   t   = t_base · m_tool · m_light · m_body · (1 − 0.4 · min(n, N) ÷ N)
 *   kcal = rate_effort · t_base · m_tool · m_light · m_body
 * </pre>
 * Practice speeds a job up and never changes its calories, so a practiced job burns the same in less time and the
 * needs stay the clock. m_light is 2 in the dark; m_body is the product of the body's slowdowns.
 */
public final class CraftMath {
    private CraftMath() {}

    public static final double DARK_LIGHT_MULT = 2.0;

    /** Game minutes the job takes. */
    public static double minutes(double baseMin, double toolMult, double lightMult, double bodyMult, double reps, int n) {
        return baseMin * toolMult * lightMult * bodyMult * Practice.timeFactor(reps, n);
    }

    /** Kilocalories the job burns: the effort rate over the base time, with no practice term. */
    public static double kcal(Effort effort, double baseMin, double toolMult, double lightMult, double bodyMult) {
        return effort.kcalPerHour * baseMin / 60.0 * toolMult * lightMult * bodyMult;
    }

    /** m_light: 2 in the dark, 1 otherwise. */
    public static double lightMult(boolean dark) { return dark ? DARK_LIGHT_MULT : 1.0; }

    /**
     * m_body, the product of the body's slowdowns: energy under 3 bolts ×1.5, Spirits over 70 ×0.9 and under 30
     * ×1.1, and the sleep debt's own factor (1 when rested).
     */
    public static double bodyMult(double energyBolts, double spirits, double sleepDebtMult) {
        double m = sleepDebtMult;
        if (energyBolts < 3) m *= 1.5;
        if (spirits > 70) m *= 0.9;
        else if (spirits < 30) m *= 1.1;
        return m;
    }
}
