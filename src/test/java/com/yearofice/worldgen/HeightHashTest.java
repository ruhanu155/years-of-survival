package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@code worldgen/height_hash_jvms} (M0): the same seed gives the same layers, bit for bit. The golden hashes
 * below were recorded on Linux x64 (OpenJDK 21); CI on Windows and macOS must match them unchanged. A change
 * here is a generator version bump, never a quiet edit.
 */
class HeightHashTest {
    /** levelSeed, attempt that passed, landform hash, height hash (eroded). */
    static final long[][] GOLDEN = {
        {1L, 0, 0x256d4f9dL, 0xc0c2bb20L},
        {42L, 0, 0xb9190a8dL, 0x8ce97931L},
        {3141592L, 0, 0x95b627d5L, 0x72b9660bL},
    };

    @Test
    void goldenHashes() {
        for (long[] g : GOLDEN) {
            Fixture.Summary s = Fixture.of(g[0]);
            String at = "seed " + g[0];
            assertEquals((int) g[1], s.attempt(), at + " attempt");
            assertEquals(Integer.toHexString((int) g[2]), Integer.toHexString(s.landformHash()), at + " landform hash");
            assertEquals(Integer.toHexString((int) g[3]), Integer.toHexString(s.heightHash()), at + " height hash");
        }
    }

    @Test
    void sameSeedSameHashInOneJvm() {
        Pipeline.Result a = Fixture.PIPE.run(7L);
        Pipeline.Result b = Fixture.PIPE.run(7L);
        assertEquals(a.landformHash(), b.landformHash());
        assertEquals(a.heightHash(), b.heightHash());
        assertEquals(a.erosion().sedimentHash(), b.erosion().sedimentHash());
    }

    @Test
    void stageSeedsChainFromTheLevelSeed() {
        // seed = hash(levelSeed, "yearofice"); attemptSeed = hash(seed, k); stage = hash(attemptSeed, name).
        int ours = SeedStage.ourSeed(3141592L);
        assertEquals(com.yearofice.core.hash.StageHash.of("3141592:yearofice"), ours);
        int att = SeedStage.attemptSeed(ours, 0);
        assertEquals(com.yearofice.core.hash.StageHash.of(Integer.toUnsignedString(ours) + ":0"), att);
        assertEquals(com.yearofice.core.hash.StageHash.of(Integer.toUnsignedString(att) + ":landform"),
                SeedStage.stageSeed(att, Landform.STAGE));
    }
}
