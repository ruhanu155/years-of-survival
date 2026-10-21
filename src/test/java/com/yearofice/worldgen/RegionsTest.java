package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import com.yearofice.worldgen.regions.Regions;
import org.junit.jupiter.api.Test;

class RegionsTest {
    @Test void sixteenRegionsTileRingOne() {
        assertEquals(16, Regions.all().size());
        assertEquals("A1", Regions.at(-4096, -4096).cell());
        assertEquals("D4", Regions.at(4095, 4095).cell());
        assertEquals("B3", Regions.at(-1, 1).cell());
        assertEquals("sheep_crags", Regions.byId("A1").path());
        assertNull(Regions.at(4096, 0));
        for (Regions.Region r : Regions.all()) {
            assertEquals(r.cell(), Regions.at(r.centreX(), r.centreZ()).cell());
            assertEquals(1.0, r.biomeMix().values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9, r.id());
        }
    }

    @Test void ringsAndBorderFromData() {
        assertEquals(1, Regions.ring(4095, -4096));
        assertEquals(2, Regions.ring(4096, 0));
        assertEquals(3, Regions.ring(40000, 0));
        assertEquals(8192, Regions.borderSize(1));
        assertEquals(1, Regions.defaultRing());
    }

    @Test void outpostsFollowRegions() {
        assertTrue(Regions.allowsOutpost(0, 1, "fish_camp"));
        assertFalse(Regions.allowsOutpost(-4000, -4000, "fish_camp")); // A1: crags
        assertTrue(Regions.allowsOutpost(9000, 0, "fish_camp")); // ring 2: unrestricted
    }
}
