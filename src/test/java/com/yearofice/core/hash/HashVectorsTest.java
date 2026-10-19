package com.yearofice.core.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Reference vectors computed independently in Python; any JVM must match them bit for bit. */
class HashVectorsTest {
    @Test
    void mulberry32MatchesReference() {
        Mulberry32 a = new Mulberry32(1);
        assertEquals(2693262067L, a.nextUInt());
        assertEquals(11749833L, a.nextUInt());
        assertEquals(2265367787L, a.nextUInt());
        Mulberry32 b = new Mulberry32(3141592);
        assertEquals(2113771094L, b.nextUInt());
        assertEquals(1380401252L, b.nextUInt());
        assertEquals(2725156298L, b.nextUInt());
    }

    @Test
    void mulberry32DoubleInRange() {
        Mulberry32 r = new Mulberry32(42);
        for (int i = 0; i < 10_000; i++) {
            double d = r.nextDouble();
            if (d < 0 || d >= 1) throw new AssertionError("out of range: " + d);
        }
    }

    @Test
    void fnv1aMatchesReference() {
        assertEquals(0x811C9DC5, StageHash.of(""));
        assertEquals(0x572A6BB8, StageHash.of("yearofice"));
    }
}
