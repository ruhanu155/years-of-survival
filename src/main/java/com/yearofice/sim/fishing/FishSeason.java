package com.yearofice.sim.fishing;

/**
 * m_season for one species (Blueprint › Fishing in depth › Season presence; Conditions, m_season row):
 * 1.0 inside the species' When window, 0.5 in its first and last month, ×3 at a spawning run at that spot.
 * Outside the window resident fish count 0.3 at that spot (they have moved) and salmon are absent (0).
 *
 * <p>Days count the game year from May 1 (Blueprint › Open decisions, Year length: 365 days from May 1), so day 1
 * is May 1 and day 365 is April 30. Windows may wrap the year end (the grayling spawn runs April 25 to May 31).
 *
 * <p>Pure Java. Original Year of Ice code.
 *
 * @param fromDay     first day of the When window (1–365)
 * @param toDay       last day of the When window
 * @param allYear     true when the species is present all year (no first or last month)
 * @param resident    true for resident fish (0.3 outside the window), false for salmon (0)
 * @param spawnSite   the spot type the spawn runs at, or null
 * @param spawnFrom   first day of the spawn at that site
 * @param spawnTo     last day of the spawn
 */
public record FishSeason(int fromDay, int toDay, boolean allYear, boolean resident, String spawnSite,
                         int spawnFrom, int spawnTo) {

    public static final double IN_WINDOW = 1.0, EDGE_MONTH = 0.5, RESIDENT_OUTSIDE = 0.3, SPAWN_RUN = 3.0;

    /** Month lengths from May: May 31, Jun 30, Jul 31, Aug 31, Sep 30, Oct 31, Nov 30, Dec 31, Jan 31, Feb 28, Mar 31, Apr 30. */
    private static final int[] LEN_FROM_MAY = {31, 30, 31, 31, 30, 31, 30, 31, 31, 28, 31, 30};

    /** The game-year day (1–365) of a calendar date. */
    public static int day(int month, int dayOfMonth) {
        int idx = Math.floorMod(month - 5, 12);
        int d = 0;
        for (int i = 0; i < idx; i++) d += LEN_FROM_MAY[i];
        return d + dayOfMonth;
    }

    /** The calendar month (1–12) of a game-year day; days outside 1–365 wrap. */
    public static int month(int day) {
        int d = Math.floorMod(day - 1, 365);
        for (int i = 0; i < 12; i++) {
            if (d < LEN_FROM_MAY[i]) return Math.floorMod(i + 4, 12) + 1;
            d -= LEN_FROM_MAY[i];
        }
        return 4;
    }

    /** The game-year day (1–365) of an absolute day count that starts at 1 on the first May 1. */
    public static int dayOfYear(long absoluteDay) { return (int) Math.floorMod(absoluteDay - 1, 365L) + 1; }

    static boolean within(int day, int from, int to) {
        return from <= to ? day >= from && day <= to : day >= from || day <= to;
    }

    public boolean inWindow(int day) { return allYear || within(day, fromDay, toDay); }

    public boolean spawning(int day, String spotType) {
        return spawnSite != null && spawnSite.equals(spotType) && within(day, spawnFrom, spawnTo);
    }

    /** m_season on a game-year day at a spot of the given type. */
    public double factor(int day, String spotType) {
        double m;
        if (inWindow(day)) {
            int mo = month(day);
            m = !allYear && (mo == month(fromDay) || mo == month(toDay)) ? EDGE_MONTH : IN_WINDOW;
        } else {
            m = resident ? RESIDENT_OUTSIDE : 0.0;
        }
        return spawning(day, spotType) ? m * SPAWN_RUN : m;
    }

    /** Arctic grayling: May–September; spawns at creek mouths from breakup (April 25) to May 31. */
    public static FishSeason arcticGrayling() {
        return new FishSeason(day(5, 1), day(9, 30), false, true, "creek_mouth", day(4, 25), day(5, 31));
    }
}
