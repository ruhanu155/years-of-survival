package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.plant.PlantHabitat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** The vegetation pass, pure: patches by habitat, the same whichever chunk asks first, and the start site's own patch. */
class PlantPlanTest {
    static final double DENSITY = 0.35;

    @Test
    void plantsStandOnTheirHabitatAndSpeciesMix() {
        for (long seed : M2WorldTest.SEEDS) {
            ValleyWorld w = M2WorldTest.world(seed);
            int[] s = w.spawn();
            List<PlantPlan.Clump> all = PlantPlan.clumpsIn(w, s[0] - 128, s[2] - 128, s[0] + 128, s[2] + 128, DENSITY);
            TreeSet<String> kinds = new TreeSet<>();
            var entries = PlantHabitat.load(com.yearofice.plant.PlantCatalog.SPECIES);
            for (PlantPlan.Clump c : all) {
                kinds.add(c.species());
                if (PlantPlan.START_PATCH_IDS.contains(c.species())) continue;
                PlantHabitat.Site site = PlantPlan.site(w, c.x(), c.z());
                var e = entries.stream().filter(en -> en.id().equals(c.species())).findFirst().orElseThrow();
                assertTrue(PlantHabitat.fitsAny(e, new PlantHabitat.Site(site.cls(), 10, true, true, site.startDistM())), seed + ": " + c + " on " + site.cls());
            }
            assertTrue(all.size() >= 300, seed + ": only " + all.size() + " clumps in 256 x 256");
            assertTrue(kinds.size() >= 8, seed + ": only " + kinds.size() + " species");
        }
    }

    @Test
    void sameWhicheverChunkAsksFirst() {
        ValleyWorld w = M2WorldTest.world(3141592);
        int[] s = w.spawn();
        int x0 = (s[0] >> 4) * 16, z0 = (s[2] >> 4) * 16;
        List<PlantPlan.Clump> whole = PlantPlan.clumpsIn(w, x0, z0, x0 + 32, z0 + 32, DENSITY);
        List<PlantPlan.Clump> parts = new ArrayList<>();
        for (int dz = 0; dz < 32; dz += 16) for (int dx = 0; dx < 32; dx += 16) parts.addAll(PlantPlan.clumpsIn(w, x0 + dx, z0 + dz, x0 + dx + 16, z0 + dz + 16, DENSITY));
        assertEquals(new TreeSet<>(whole.stream().map(Object::toString).toList()), new TreeSet<>(parts.stream().map(Object::toString).toList()));
        assertEquals(whole, PlantPlan.clumpsIn(w, x0, z0, x0 + 32, z0 + 32, DENSITY));
    }

    @Test
    void startSiteKeepsABerryAndGreensWithin50m() {
        for (long seed : M2WorldTest.SEEDS) {
            ValleyWorld w = M2WorldTest.world(seed);
            var st = w.start();
            if (st == null) continue;
            List<PlantPlan.Clump> near = PlantPlan.clumpsIn(w, (int) st.x() - 50, (int) st.z() - 50, (int) st.x() + 50, (int) st.z() + 50, 0.0);
            boolean berry = false, greens = false;
            for (PlantPlan.Clump c : near) {
                if (Math.hypot(c.x() - st.x(), c.z() - st.z()) > 50) continue;
                berry |= c.species().endsWith("blueberry_bush");
                greens |= c.species().endsWith("fireweed") || c.species().endsWith("dandelion");
            }
            assertTrue(berry && greens, seed + ": berry " + berry + ", greens " + greens);
        }
    }
}
