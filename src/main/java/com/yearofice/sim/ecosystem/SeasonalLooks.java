package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;

/**
 * Looks by date (Land animals in depth › Seasonal looks; Birds and the air › Grouse and ptarmigan). The server
 * sets the look bytes from these; the client never computes a date. Pure Java.
 *
 * <ul>
 *   <li>Snowshoe hare, "brown, then white Oct – Apr" over 20 days of mottled texture: summer May 1 – Sep 30,
 *       mottled Oct 1 – 20, winter Oct 21 – Apr 10, mottled Apr 11 – 30.</li>
 *   <li>Willow ptarmigan, white Oct 20 – Apr 20, the moult blending over 21 days on each side: moult Sep 29 –
 *       Oct 19 and Apr 21 – May 11.</li>
 *   <li>Bull moose antlers: shed by Dec 15 (prime bulls shed Dec 1 – Jan 15), regrowth from Apr 15, full size
 *       when the velvet strips on Aug 25; the scale grows linearly between.</li>
 * </ul>
 */
public final class SeasonalLooks {
    private SeasonalLooks() {}

    /** Texture variants, in the order of the coat byte. */
    public enum Coat {
        SUMMER("summer"), MOTTLED("mottled"), WINTER("winter"), MOULT("moult");

        public final String key;

        Coat(String key) { this.key = key; }

        public static Coat byId(int id) {
            Coat[] v = values();
            return id >= 0 && id < v.length ? v[id] : SUMMER;
        }
    }

    static final int HARE_TURN_START = Calendar.dayOfYear(10, 1), HARE_WHITE = Calendar.dayOfYear(10, 21),
            HARE_BROWN_START = Calendar.dayOfYear(4, 11), HARE_WHITE_END = Calendar.dayOfYear(4, 10),
            HARE_TURN_END = Calendar.dayOfYear(10, 20), HARE_BROWN_END = Calendar.dayOfYear(4, 30);

    public static Coat hare(int doy) {
        if (Calendar.within(doy, HARE_TURN_START, HARE_TURN_END)) return Coat.MOTTLED;
        if (Calendar.within(doy, HARE_WHITE, HARE_WHITE_END)) return Coat.WINTER;
        if (Calendar.within(doy, HARE_BROWN_START, HARE_BROWN_END)) return Coat.MOTTLED;
        return Coat.SUMMER;
    }

    static final int PTARMIGAN_MOULT_IN = Calendar.dayOfYear(9, 29), PTARMIGAN_WHITE = Calendar.dayOfYear(10, 20),
            PTARMIGAN_WHITE_END = Calendar.dayOfYear(4, 20), PTARMIGAN_MOULT_OUT_END = Calendar.dayOfYear(5, 11);

    public static Coat ptarmigan(int doy) {
        if (Calendar.within(doy, PTARMIGAN_WHITE, PTARMIGAN_WHITE_END)) return Coat.WINTER;
        if (Calendar.within(doy, PTARMIGAN_MOULT_IN, PTARMIGAN_WHITE - 1)) return Coat.MOULT;
        if (Calendar.within(doy, PTARMIGAN_WHITE_END + 1, PTARMIGAN_MOULT_OUT_END)) return Coat.MOULT;
        return Coat.SUMMER;
    }

    static final int ANTLER_REGROW = Calendar.dayOfYear(4, 15), ANTLER_FULL = Calendar.dayOfYear(8, 25),
            ANTLER_SHED = Calendar.dayOfYear(12, 15);

    /** Bull antler scale, 0 to 1. */
    public static double antlers(int doy) {
        if (Calendar.within(doy, ANTLER_FULL, ANTLER_SHED)) return 1.0;
        if (Calendar.within(doy, ANTLER_SHED + 1, ANTLER_REGROW)) return 0.0;
        int span = Math.floorMod(ANTLER_FULL - ANTLER_REGROW, Calendar.DAYS_PER_YEAR);
        int into = Math.floorMod(doy - ANTLER_REGROW, Calendar.DAYS_PER_YEAR);
        return Math.min(1.0, (double) into / span);
    }
}
