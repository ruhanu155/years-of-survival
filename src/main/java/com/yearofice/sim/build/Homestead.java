package com.yearofice.sim.build;

import com.yearofice.sim.time.Calendar;

/**
 * The Homestead gate (Tiers and gates): the first river breakup the player lives through while owning a warm room with
 * a stove or fireplace. Breakup comes about April 26; the gate looks at the player for the three weeks around it.
 * Pure Java; original Year of Ice code.
 */
public final class Homestead {
    private Homestead() {}

    public static final int BREAKUP_DOY = Calendar.dayOfYear(4, 26);
    public static final int WINDOW_DAYS = 21;

    /** Is {@code doy} inside the breakup window? */
    public static boolean inWindow(int doy) { return doy >= BREAKUP_DOY && doy < BREAKUP_DOY + WINDOW_DAYS; }

    /** The gate opens when the player stands in a warm room during the window. */
    public static boolean qualifies(int doy, boolean warmRoom) { return warmRoom && inWindow(doy); }
}
