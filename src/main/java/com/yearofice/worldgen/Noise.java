package com.yearofice.worldgen;

/**
 * Deterministic noise for the generator. Pure Java, no Minecraft imports, no tables: every lattice
 * gradient comes from an integer hash of (seed, i, j), so a noise field is fully named by its seed.
 *
 * <p>Only {@code + − × ÷}, {@link Math#floor} and comparisons are used, all exactly rounded in Java,
 * so every JVM returns the same bits (Minecraft engine › Determinism).
 */
public final class Noise {
    private Noise() {}

    // 2D simplex skew factors: (sqrt(3) - 1) / 2 and (3 - sqrt(3)) / 6, written as literals.
    private static final double F2 = 0.36602540378443864676;
    private static final double G2 = 0.21132486540518711775;
    private static final double INV_SQRT2 = 0.70710678118654752440;

    /** 32-bit integer mix of a seed and two lattice coordinates (murmur3 finaliser). */
    public static int hash(int seed, int i, int j) {
        int h = seed ^ (i * 0x27D4EB2D) ^ (j * 0x165667B1);
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }

    /** Dot of the hashed unit gradient at (i, j) with (x, y). Eight directions, all of length 1. */
    private static double grad(int seed, int i, int j, double x, double y) {
        switch (hash(seed, i, j) & 7) {
            case 0: return x;
            case 1: return -x;
            case 2: return y;
            case 3: return -y;
            case 4: return (x + y) * INV_SQRT2;
            case 5: return (-x + y) * INV_SQRT2;
            case 6: return (x - y) * INV_SQRT2;
            default: return (-x - y) * INV_SQRT2;
        }
    }

    /** 2D simplex noise in about [-1, 1]. */
    public static double simplex(int seed, double x, double y) {
        double s = (x + y) * F2;
        int i = (int) Math.floor(x + s);
        int j = (int) Math.floor(y + s);
        double t = (i + j) * G2;
        double x0 = x - (i - t);
        double y0 = y - (j - t);
        int i1 = x0 > y0 ? 1 : 0;
        int j1 = 1 - i1;
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        double n = 0.0;
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 > 0) { t0 *= t0; n += t0 * t0 * grad(seed, i, j, x0, y0); }
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 > 0) { t1 *= t1; n += t1 * t1 * grad(seed, i + i1, j + j1, x1, y1); }
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 > 0) { t2 *= t2; n += t2 * t2 * grad(seed, i + 1, j + 1, x2, y2); }
        return 99.0 * n; // scales the peak of unit-gradient simplex to about 1
    }

    /**
     * Fractal Brownian motion of simplex noise: {@code octaves} layers, frequency x2 and amplitude x0.5
     * per octave, normalised back to about [-1, 1]. Each octave has its own derived seed.
     */
    public static double fbm(int seed, double x, double y, int octaves) {
        double sum = 0.0, amp = 1.0, norm = 0.0, f = 1.0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * simplex(seed + o * 0x9E3779B9, x * f, y * f);
            norm += amp;
            amp *= 0.5;
            f *= 2.0;
        }
        return sum / norm;
    }

    /**
     * Ridged multifractal in [0, 1]: each octave is {@code (1 - |n|)^2}, weighted by the previous
     * octave so ridges stay sharp and valleys stay smooth.
     */
    public static double ridged(int seed, double x, double y, int octaves) {
        double sum = 0.0, amp = 1.0, norm = 0.0, f = 1.0, weight = 1.0;
        for (int o = 0; o < octaves; o++) {
            double n = simplex(seed + o * 0x9E3779B9, x * f, y * f);
            double r = 1.0 - Math.abs(n);
            if (r < 0) r = 0;
            r *= r;
            r *= weight;
            weight = clamp01(r * 2.0);
            sum += amp * r;
            norm += amp;
            amp *= 0.5;
            f *= 2.0;
        }
        return clamp01(sum / norm);
    }

    /** 1D gradient noise in about [-1, 1] with a quintic fade. */
    public static double noise1(int seed, double x) {
        int i = (int) Math.floor(x);
        double f = x - i;
        double g0 = gradient1(seed, i);
        double g1 = gradient1(seed, i + 1);
        double v0 = g0 * f;
        double v1 = g1 * (f - 1.0);
        double u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        return 2.0 * (v0 + (v1 - v0) * u);
    }

    private static double gradient1(int seed, int i) {
        int h = hash(seed, i, 0x5BD1E995);
        return ((h >>> 8) / 8388608.0) - 1.0; // 24-bit value mapped to [-1, 1)
    }

    /** Two-octave sum of {@link #noise1}, normalised to about [-1, 1]. */
    public static double fbm1(int seed, double x, int octaves) {
        double sum = 0.0, amp = 1.0, norm = 0.0, f = 1.0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise1(seed + o * 0x9E3779B9, x * f);
            norm += amp;
            amp *= 0.5;
            f *= 2.0;
        }
        return sum / norm;
    }

    public static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }

    /** Cubic smoothstep of t clamped to [0, 1]. */
    public static double smooth(double t) {
        t = clamp01(t);
        return t * t * (3.0 - 2.0 * t);
    }

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
