package com.yearofice.core.hash;

/**
 * Mulberry32 PRNG. Pure Java, no Minecraft imports, bit-identical across JVMs.
 * The blueprint's generation stages draw every random number from this so a seed replays exactly.
 */
public final class Mulberry32 {
    private int state;

    public Mulberry32(int seed) { this.state = seed; }

    /** Next unsigned 32-bit value, returned in a long. */
    public long nextUInt() {
        state += 0x6D2B79F5;
        int t = state;
        t = (t ^ (t >>> 15)) * (t | 1);
        t ^= t + (t ^ (t >>> 7)) * (t | 61);
        return (t ^ (t >>> 14)) & 0xFFFFFFFFL;
    }

    /** Next double in [0, 1). */
    public double nextDouble() { return nextUInt() / 4294967296.0; }

    /** Next int in [0, bound). */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return (int) (nextDouble() * bound);
    }
}
