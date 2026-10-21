package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;

/**
 * Seasonal dormancy dates (Blueprint › Drives and daily schedules, Seasonal drives; Bestiary › Wood frog): bears den
 * October 20 to April 20, wood frogs lie frozen October 15 to April 25.
 */
public final class Dormancy {
    private Dormancy() {}

    static final int DEN_FROM = Calendar.dayOfYear(10, 20), DEN_TO = Calendar.dayOfYear(4, 20);
    static final int FROG_FROM = Calendar.dayOfYear(10, 15), FROG_TO = Calendar.dayOfYear(4, 25);

    public static boolean denned(String species, int doy) {
        return (species.endsWith("black_bear") || species.endsWith("grizzly")) && Calendar.within(doy, DEN_FROM, DEN_TO);
    }

    public static boolean frozenFrog(int doy) { return Calendar.within(doy, FROG_FROM, FROG_TO); }
}
