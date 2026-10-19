package com.yearofice.sim.decay;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;

/**
 * Thresholds, not rolls (Decay and time › Catch-up and determinism, rule 3).
 *
 * <p>Anything that happens once, such as a roof block falling, draws its own threshold u when placed:
 * hash(seed, "decay", position, placed day). It happens when p passes u. No roll depends on when the player
 * looked, so a chunk loaded all year and one loaded at its end agree.
 *
 * <p>{@link #stream} is the named-stream helper for the other runtime rolls in these cores (mice, traps,
 * meals). It follows the lead's rule (keys are positions, record ids and game days, never tick numbers or
 * call order) and stands in for {@code core.hash.Rng.of} until the lead engineer lands it; the key string
 * format is the same FNV-1a over colon-joined parts that {@link StageHash#stageSeed} uses.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class Thresholds {
    private Thresholds() {}

    /** Load-bearing blocks fail between these (Decay and time › Wood, roofs are wood with load). */
    public static final double LOAD_U_MIN = 0.35;
    public static final double LOAD_U_MAX = 0.55;

    /** A named stream: FNV-1a over "seed:system:k1:k2:..." seeds a Mulberry32. */
    public static Mulberry32 stream(long seed, String system, long... keys) {
        StringBuilder sb = new StringBuilder(32).append(seed).append(':').append(system);
        for (long k : keys) sb.append(':').append(k);
        return new Mulberry32(StageHash.of(sb.toString()));
    }

    /** A uniform draw in [0, 1) for a placed thing. */
    public static double u(long seed, int x, int y, int z, long placedDay) {
        return stream(seed, "decay", x, y, z, placedDay).nextDouble();
    }

    /** A threshold in [lo, hi) for a placed thing. */
    public static double threshold(long seed, int x, int y, int z, long placedDay, double lo, double hi) {
        return lo + (hi - lo) * u(seed, x, y, z, placedDay);
    }

    /** The load threshold of a load-bearing block, between 0.35 and 0.55. */
    public static double loadThreshold(long seed, int x, int y, int z, long placedDay) {
        return threshold(seed, x, y, z, placedDay, LOAD_U_MIN, LOAD_U_MAX);
    }
}
