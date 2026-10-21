package com.yearofice.sim.story;

/** The radio's arithmetic (Open world in depth › The radio): battery charge by month, flying weather. No Minecraft imports. */
public final class RadioMath {
    private RadioMath() {}

    /** Percent a sunny day adds to the radio battery: May–Aug 10, Mar/Apr/Sep/Oct 4, Nov–Feb 1; a dull day gives a third. */
    public static double dailyGainPct(int month, boolean sunny, double panelFactor) {
        double base = switch (month) {
            case 5, 6, 7, 8 -> 10;
            case 3, 4, 9, 10 -> 4;
            default -> 1;
        };
        return base * (sunny ? 1 : 1.0 / 3) * panelFactor;
    }

    /** Pete flies only in clear or cloudy weather with wind under 8 m/s. */
    public static boolean canFly(boolean calmSky, double windMs) { return calmSky && windMs < 8; }

    /** A pick in [0, n) that depends only on the inputs, for forecasts and news that must read the same twice. */
    public static int pick(long a, long b, int n) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 29;
        return (int) Math.floorMod(h, (long) n);
    }

    /** Dana's days 3–5 are right 70% of the time (a pure function of the day and lead). */
    public static boolean outlookRight(long absDay, int lead) { return lead < 3 || pick(absDay, lead, 100) < 70; }
}
