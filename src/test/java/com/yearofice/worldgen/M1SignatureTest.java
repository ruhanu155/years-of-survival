package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * M1: each archetype shows its signature (braided: 3 or more channels over at least 500 m; big lake: one lake
 * of 0.2 km² or more; canyon: 4 or more falls; marsh: 2 or more oxbow lakes), and every seed meets its
 * archetype's minimum falls. Seeds 0–15 hold each archetype four times.
 */
class M1SignatureTest {
    @Test
    void eachArchetypeShowsItsSignature() {
        Map<Archetype, Integer> seen = new EnumMap<>(Archetype.class);
        for (long seed = 0; seed < 16; seed++) {
            Fixture.Summary s = Fixture.of(seed);
            M1Summary m = M1WaterTest.m1(seed);
            String at = "seed " + seed + " (" + s.archetype().path() + ")";
            seen.merge(s.archetype(), 1, Integer::sum);
            switch (s.archetype()) {
                case BRAIDED_FLATS -> {
                    assertTrue(m.channels() >= 3, at + ": " + m.channels() + " channels");
                    assertTrue(m.braidRunM() >= 500, at + ": 3+ channels over only " + m.braidRunM() + " m");
                }
                case LAKE_VALLEY -> assertTrue(m.largestLakeM2() >= 200_000, at + ": largest lake " + m.largestLakeM2() + " m2");
                case CANYON -> assertTrue(m.falls() >= 4, at + ": " + m.falls() + " falls");
                case OXBOW_MARSH -> assertTrue(m.oxbowLakes() >= 2, at + ": " + m.oxbowLakes() + " oxbow lakes");
            }
            System.out.printf("%s: %d channel(s), braids over %.0f m, largest lake %.3f km2, %d falls, %d oxbow lakes%n", at,
                    m.channels(), m.braidRunM(), m.largestLakeM2() / 1e6, m.falls(), m.oxbowLakes());
        }
        for (Archetype a : Archetype.values()) assertEquals(4, seen.getOrDefault(a, 0), a + " seeds");
    }

    @Test
    void everySeedMeetsItsArchetypesMinimumFalls() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.falls() >= m.minimumFalls(), "seed " + seed + ": " + m.falls() + " falls, minimum " + m.minimumFalls());
        }
    }
}
