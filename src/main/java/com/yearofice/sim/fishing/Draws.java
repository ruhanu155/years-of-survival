package com.yearofice.sim.fishing;

/**
 * Uniform draws for the fishing adapter's rolls (the bite each game minute, the cue's lead, the strike, the size),
 * taken from a hash of (seed, a, b, salt) and never from call order, so a roll replays the same whatever ran
 * before it. Same finaliser as {@link FightModel}'s draws (SplitMix64), pure integer maths.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class Draws {
    private Draws() {}

    public static double u(long seed, long a, long b, int salt) {
        long z = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ ((long) salt * 0xD1B54A32D192ED03L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * 0x1.0p-53;
    }

    /** A string folded to a long (FNV-1a), for named streams such as {@code "bite"}. */
    public static long name(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
