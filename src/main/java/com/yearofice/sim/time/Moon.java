package com.yearofice.sim.time;

/**
 * {@code sim.time.Moon}: the phase on a 29.53-day cycle from a seeded offset ({@code hash(seed, "moon")}, Layer 1).
 * Phase 0 is new, 0.5 full. Rise and height come with the moon table and the custom sky (M7).
 */
public final class Moon {
    private Moon() {}

    public static final double SYNODIC_DAYS = 29.53;

    /** Phase 0–1 at absolute game minute {@code minute}, with the world's offset in days. */
    public static double phase(long minute, double offsetDays) {
        double days = minute / (double) Calendar.MINUTES_PER_DAY + offsetDays;
        double p = days / SYNODIC_DAYS;
        return p - Math.floor(p);
    }

    /** The world's offset in days from a 32-bit hash. */
    public static double offsetDays(int hash) { return Integer.toUnsignedLong(hash) / 4294967296.0 * SYNODIC_DAYS; }
}
