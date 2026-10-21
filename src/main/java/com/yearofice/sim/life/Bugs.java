package com.yearofice.sim.life;

import com.yearofice.sim.time.Calendar;

/**
 * Bug pressure for the three biting-insect swarms (Blueprint › Bugs: B = date x habitat x time x wind x temperature;
 * Bestiary › Black flies and no-see-ums). Pure Java. Original Year of Ice code.
 */
public final class Bugs {
    private Bugs() {}

    public enum Kind {
        MOSQUITO(6, 10, 6, 20, 7, 31),
        BLACK_FLY(6, 1, 6, 10, 8, 15),
        NO_SEE_UM(7, 1, 7, 10, 8, 31);

        final int riseMonth, riseDay, fullMonth, fullDay, endMonth, endDay;
        final int fullEndMonth, fullEndDay;

        Kind(int rm, int rd, int fm, int fd, int em, int ed) {
            riseMonth = rm; riseDay = rd; fullMonth = fm; fullDay = fd; endMonth = em; endDay = ed;
            // each plateau ends 16 days before the season does (mosquito: Jul 15 to Jul 31)
            int e = Calendar.dayOfYear(em, ed) - 16;
            fullEndMonth = Calendar.month(e);
            fullEndDay = Calendar.dayOfMonth(e);
        }

        public String path() { return name().toLowerCase(java.util.Locale.ROOT); }

        public static Kind of(String s) {
            for (Kind k : values()) if (k.path().equals(s)) return k;
            return null;
        }
    }

    /** The date term: 0 before the season, rising to 1 over ten days, 1 on the plateau, falling to 0 by the end. */
    public static double date(Kind k, int doy) {
        int rise = Calendar.dayOfYear(k.riseMonth, k.riseDay), full = Calendar.dayOfYear(k.fullMonth, k.fullDay);
        int fullEnd = Calendar.dayOfYear(k.fullEndMonth, k.fullEndDay), end = Calendar.dayOfYear(k.endMonth, k.endDay);
        if (doy < rise || doy > end) return 0;
        if (doy < full) return (doy - rise) / (double) Math.max(1, full - rise);
        if (doy <= fullEnd) return 1;
        return (end - doy) / (double) Math.max(1, end - fullEnd);
    }

    /** Habitat term: bog 1.0, within 100 m of still water 0.8, tundra and riparian 0.6, spruce taiga 0.5, birch slope 0.4, gravel bar 0.3. */
    public static double habitat(String h) {
        return switch (h) {
            case "bog" -> 1.0;
            case "pond_edge", "still_water" -> 0.8;
            case "tundra", "riparian", "bank" -> 0.6;
            case "forest", "spruce_edge", "taiga" -> 0.5;
            case "birch_slope" -> 0.4;
            case "gravel_bar" -> 0.3;
            default -> 0.4;
        };
    }

    /** Dawn and dusk 1.5, night 1.0, midday 0.6. */
    public static double time(int minuteOfDay) {
        int h = minuteOfDay / 60;
        if ((h >= 5 && h < 8) || (h >= 19 && h < 23)) return 1.5;
        if (h >= 23 || h < 5) return 1.0;
        return 0.6;
    }

    public static double wind(double ms) { return Math.max(0, 1 - ms / 4.0); }

    /** 0 below 8 C, 1 above 12 C, a straight line between. */
    public static double temp(double c) { return Math.max(0, Math.min(1, (c - 8) / 4.0)); }

    public static double pressure(Kind k, int doy, String habitat, int minuteOfDay, double windMs, double airC) {
        return date(k, doy) * habitat(habitat) * time(minuteOfDay) * wind(windMs) * temp(airC);
    }

    /** The player is exposed at 0.3 or more with nothing protecting them. */
    public static boolean exposed(double b, boolean protectedFromBugs) { return b >= 0.3 && !protectedFromBugs; }

    /** Game seconds between bites: the base interval at B = 1, longer as B falls; infinite under 0.3. */
    public static double biteIntervalS(double b, double baseS) { return b < 0.3 ? Double.POSITIVE_INFINITY : baseS / b; }
}
