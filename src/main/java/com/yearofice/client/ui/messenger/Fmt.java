package com.yearofice.client.ui.messenger;

import java.util.Locale;

/** Number formatting for the messenger, metric first (the Units rule of Layer 8; {@code client.ui.Units} later). */
public final class Fmt {
    private Fmt() {}

    public static String kg(double v) { return String.format(Locale.ROOT, "%.1f kg", v); }

    public static String one(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    public static String whole(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    public static String signed(double v) { return String.format(Locale.ROOT, "%+,d", Math.round(v)); }

    public static String pct(double share) { return Math.round(share * 100) + "%"; }

    /** A temperature, with a real minus sign. */
    public static String c(double v) {
        long r = Math.round(v);
        return (r < 0 ? "−" + (-r) : Long.toString(r)) + " °C";
    }

    /** A temperature term, always signed. */
    public static String dc(double v) {
        long r = Math.round(v);
        return (r < 0 ? "−" + (-r) : "+" + r) + " °C";
    }

    public static String hhmm(long minute) {
        int m = (int) Math.floorMod(minute, 1440);
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }

    public static int day(long minute) { return (int) Math.floorDiv(minute, 1440) + 1; }

    public static String litres(double ml) { return String.format(Locale.ROOT, "%.2f L", ml / 1000.0); }
}
