package com.yearofice.sim.livestock;

/**
 * The juvenile growth model (Raising animals, Raising orphaned young). A young animal carries a progress {@code p} from
 * 0 (born) to 1 (adult). Each day it advances by {@code growthScale / matureDays} times a feed factor, so a hungry
 * year stunts it instead of just delaying a birthday. Mass follows {@code birth + (adult − birth)(1 − (1 − p)²)}, the
 * fast early growth of a kid or a calf. Until weaning it needs milk; without it, it dies on the schedule below.
 */
public final class Growth {
    private Growth() {}

    public enum Stage { SUCKLING, JUVENILE, YOUNG_ADULT, ADULT }

    public static final double BREEDING_P = 0.7;

    /** Progress gained in a day at the given fed level (0–100) and whether milk was available while suckling. */
    public static double dayStep(Spec sp, LivestockTuning t, double p, double ageDays, double fed, boolean milk) {
        if (p >= 1) return 1;
        if (ageDays < sp.weanDays() && !milk) return p + 0.0;
        double f = fed >= 60 ? 1 : fed >= 30 ? 0.5 : 0.1;
        return Math.min(1, p + t.growthScale() * f / sp.matureDays());
    }

    public static double massKg(Spec sp, double p) {
        double q = Math.max(0, Math.min(1, p));
        return sp.birthKg() + (sp.adultKg() - sp.birthKg()) * (1 - (1 - q) * (1 - q));
    }

    public static Stage stage(Spec sp, double p, double ageDays) {
        if (ageDays < sp.weanDays()) return Stage.SUCKLING;
        if (p < BREEDING_P) return Stage.JUVENILE;
        return p < 1 ? Stage.YOUNG_ADULT : Stage.ADULT;
    }

    /** A suckling with no milk dies after this many days without (a calf's third day, a kit's second; a kid's third). */
    public static final int STARVE_DAYS_SUCKLING = 3;

    /** Days a young animal on steady feeding needs to reach 100% (ignores a missed meal). */
    public static double daysToAdult(Spec sp, LivestockTuning t, double feedFactor) { return sp.matureDays() / (t.growthScale() * feedFactor); }

    /** Calendar births: the day of year (0 = May 1) a female kids, or −1 when she is not in kidding season. */
    public static boolean inBirthMonth(Spec sp, int month) { return month == sp.birthMonth(); }

    public static boolean inRut(Spec sp, int month) {
        return sp.rutFromMonth() <= sp.rutToMonth() ? month >= sp.rutFromMonth() && month <= sp.rutToMonth()
                : month >= sp.rutFromMonth() || month <= sp.rutToMonth();
    }
}
