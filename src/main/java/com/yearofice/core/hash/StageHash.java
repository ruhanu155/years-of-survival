package com.yearofice.core.hash;

import java.nio.charset.StandardCharsets;

/** 32-bit FNV-1a, used to fingerprint generation stages and derive per-stage seeds. */
public final class StageHash {
    private static final int OFFSET = 0x811C9DC5;
    private static final int PRIME = 0x01000193;

    private StageHash() {}

    public static int of(byte[] data) {
        int h = OFFSET;
        for (byte b : data) {
            h ^= (b & 0xFF);
            h *= PRIME;
        }
        return h;
    }

    public static int of(String s) { return of(s.getBytes(StandardCharsets.UTF_8)); }

    /** FNV-1a over each float's IEEE-754 bits, little-endian: the layer fingerprint for float32 layers. */
    public static int of(float[] data) {
        int h = OFFSET;
        for (float f : data) {
            int bits = Float.floatToIntBits(f);
            for (int k = 0; k < 32; k += 8) {
                h ^= (bits >>> k) & 0xFF;
                h *= PRIME;
            }
        }
        return h;
    }

    /** Seed for a named stage of a world: FNV-1a over "seed:stage". */
    public static int stageSeed(long worldSeed, String stage) { return of(worldSeed + ":" + stage); }
}
