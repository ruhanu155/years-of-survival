package com.yearofice.sim.livestock;

/** Eggs, milk and hair (Raising animals › Production, reconciled). */
public final class Produce {
    private Produce() {}

    /** The fed factor: 1 above fed 60, 0.5 from 30 to 60, 0 below 30. */
    public static double fedFactor(double fed) { return fed >= 60 ? 1 : fed >= 30 ? 0.5 : 0; }

    /** P_egg = 0.8 · f_light · f_temp · f_fed, a hen's eggs a day. */
    public static double eggsPerDay(double lightHours, double coopC, double fed) {
        double light = Math.max(0, Math.min(1, (lightHours - 10) / 4));
        double temp = coopC < 0 ? 0 : coopC < 5 ? 0.5 : 1;
        return 0.8 * light * temp * fedFactor(fed);
    }

    /** Lactation factor: 1.0 at kidding falling linearly to 0.5 on day 240, then dry. */
    public static double lactation(double daysSinceKidding, int lactationDays) {
        if (daysSinceKidding < 0 || daysSinceKidding >= lactationDays) return 0;
        return 1.0 - 0.5 * daysSinceKidding / (lactationDays - 1);
    }

    /** Litres a doe gives in a day: 2.0 L · lactation · fed factor, ×0.75 on hay alone. */
    public static double milkLPerDay(Spec sp, double daysSinceKidding, double fed, boolean hayOnly) {
        double l = sp.milkLPerDay() * lactation(daysSinceKidding, sp.lactationDays()) * fedFactor(fed);
        return hayOnly ? l * sp.hayOnlyFactor() : l;
    }

    /** True from day 241 after kidding: the doe is dry. */
    public static boolean dry(Spec sp, double daysSinceKidding) { return daysSinceKidding >= sp.lactationDays(); }
}
