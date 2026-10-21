package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;

/**
 * Butchering yields scaling (Blueprint › Butchering and yields). A table in {@code loot_table/butchering/<id>.json}
 * gives the parts of a reference animal; an individual's parts scale with its own live weight, and the fat column
 * follows the season (September to November full; bears leaving the den 15%; late-winter moose and caribou 25%; a
 * rutted-out bull in late October 50%).
 */
public final class Yields {
    private Yields() {}

    /** Parts scale with live weight; never below 10% or above 250% of the reference. */
    public static double scale(double liveKg, double referenceKg) {
        if (referenceKg <= 0) return 1;
        return Math.max(0.1, Math.min(2.5, liveKg / referenceKg));
    }

    public static double fatFactor(String species, boolean male, int doy) {
        boolean bear = species.endsWith("black_bear") || species.endsWith("grizzly");
        boolean cervid = species.endsWith("moose") || species.endsWith("caribou");
        int sep1 = Calendar.dayOfYear(9, 1), nov30 = Calendar.dayOfYear(11, 30);
        if (bear && Calendar.within(doy, Calendar.dayOfYear(4, 20), Calendar.dayOfYear(6, 15))) return 0.15;
        if (cervid && male && species.endsWith("moose") && Calendar.within(doy, Calendar.dayOfYear(10, 20), nov30)) return 0.5;
        if (cervid && Calendar.within(doy, Calendar.dayOfYear(2, 1), Calendar.dayOfYear(4, 30))) return 0.25;
        if (Calendar.within(doy, sep1, nov30)) return 1.0;
        // Summer builds up; winter wears down. A straight blend between the two anchors.
        if (Calendar.within(doy, Calendar.dayOfYear(6, 1), sep1 - 1)) return 0.5 + 0.5 * (doy - Calendar.dayOfYear(6, 1)) / (double) (sep1 - Calendar.dayOfYear(6, 1));
        return 0.6;
    }
}
