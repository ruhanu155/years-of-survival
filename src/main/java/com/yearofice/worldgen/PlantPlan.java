package com.yearofice.worldgen;

import com.yearofice.plant.PlantCatalog;
import com.yearofice.sim.plant.PlantHabitat;
import java.util.ArrayList;
import java.util.List;

/**
 * The vegetation pass (Plants and fungi › where each grows): after the cover, patches of plants drawn cell by cell
 * from the plant files' habitats. A {@value #CELL} m cell holds a patch with probability {@code density}; the patch's
 * species is picked by hash among those whose habitat fits the cover class, slope and aspect at its centre, and its
 * clumps (the file's {@code clumps} range) scatter around the centre. Everything is a hash of the cell, so any area's
 * plants come out the same whichever chunk asks first, and nothing touches the terrain, water or cover layers.
 * The start site always keeps a berry and a greens patch within 50 m.
 */
public final class PlantPlan {
    private PlantPlan() {}

    public static final int CELL = 24, REACH = 16;
    /** Species forced near the start site: a berry bush and two greens. */
    public static final List<String> START_PATCH_IDS = List.of("yearofice:blueberry_bush", "yearofice:fireweed", "yearofice:dandelion");
    static final List<String> START_PATCH = START_PATCH_IDS;

    public record Clump(int x, int z, String species) {}

    private static volatile List<PlantHabitat.Entry> entries;

    static List<PlantHabitat.Entry> entries() {
        List<PlantHabitat.Entry> e = entries;
        if (e == null) entries = e = PlantHabitat.load(PlantCatalog.SPECIES);
        return e;
    }

    /** Clumps whose column lies in [x0, x1) x [z0, z1), in a fixed order. */
    public static List<Clump> clumpsIn(ValleyWorld vw, int x0, int z0, int x1, int z1, double density) {
        List<Clump> out = new ArrayList<>();
        int seed = vw.plantSeed();
        int c0 = Math.floorDiv(x0 - REACH, CELL), c1 = Math.floorDiv(x1 + REACH, CELL);
        int k0 = Math.floorDiv(z0 - REACH, CELL), k1 = Math.floorDiv(z1 + REACH, CELL);
        List<PlantHabitat.Entry> all = entries();
        for (int ck = k0; ck <= k1; ck++) for (int ci = c0; ci <= c1; ci++) {
            int h = Noise.hash(seed, ci, ck);
            if (((h >>> 16) & 0xFFFF) / 65536.0 >= density) continue;
            int cx = ci * CELL + (h & 0x1F) % CELL, cz = ck * CELL + ((h >>> 5) & 0x1F) % CELL;
            PlantHabitat.Site site = site(vw, cx, cz);
            List<PlantHabitat.Entry> fit = new ArrayList<>();
            for (PlantHabitat.Entry e : all) if (PlantHabitat.fitsAny(e, site)) fit.add(e);
            if (fit.isEmpty()) continue;
            PlantHabitat.Entry sp = fit.get(Math.floorMod(Noise.hash(seed ^ 0x51ED27, ci, ck), fit.size()));
            scatter(vw, sp, site, cx, cz, Noise.hash(seed ^ 0x2F0B, ci, ck), x0, z0, x1, z1, out);
        }
        StartSiteView st = start(vw);
        if (st != null && st.x() + 60 >= x0 && st.x() - 60 < x1 && st.z() + 60 >= z0 && st.z() - 60 < z1) {
            int n = 0;
            for (String id : START_PATCH) {
                for (PlantHabitat.Entry e : all) {
                    if (!e.id().equals(id)) continue;
                    int a = 14 + 10 * n++;
                    int sx = st.x() + (n % 2 == 0 ? a : -a), sz = st.z() + (n % 3 == 0 ? a : -a);
                    PlantHabitat.Site site = site(vw, sx, sz);
                    scatterAt(vw, e, site, sx, sz, Noise.hash(seed ^ 0x7A11, n, 1), 8, x0, z0, x1, z1, out);
                }
            }
        }
        return out;
    }

    record StartSiteView(int x, int z) {}

    static StartSiteView start(ValleyWorld vw) {
        var s = vw.start();
        return s == null ? null : new StartSiteView((int) Math.floor(s.x()), (int) Math.floor(s.z()));
    }

    public static PlantHabitat.Site site(ValleyWorld vw, int x, int z) {
        String cls = vw.classDef(vw.classAt(x, z)).id();
        double gx = vw.groundAt(x + 1.5, z + 0.5) - vw.groundAt(x - 0.5, z + 0.5);
        double gz = vw.groundAt(x + 0.5, z + 1.5) - vw.groundAt(x + 0.5, z - 0.5);
        double slope = CoreGrid.deg(Math.sqrt(gx * gx + gz * gz) / 2);
        // The ground falls toward the south on a south-facing slope: z grows southward, so a falling z gradient faces south.
        boolean south = slope >= 3 && gz < 0 && Math.abs(gz) >= Math.abs(gx) * 0.4;
        boolean north = slope >= 3 && gz > 0 && Math.abs(gz) >= Math.abs(gx) * 0.4;
        StartSiteView st = start(vw);
        double d = st == null ? 1e9 : Math.sqrt((double) (x - st.x()) * (x - st.x()) + (double) (z - st.z()) * (z - st.z()));
        return new PlantHabitat.Site(cls, slope, south, north, d);
    }

    private static void scatter(ValleyWorld vw, PlantHabitat.Entry sp, PlantHabitat.Site site, int cx, int cz, int h,
                                int x0, int z0, int x1, int z1, List<Clump> out) {
        int n = sp.clumpsLo() + Math.floorMod(h >>> 4, sp.clumpsHi() - sp.clumpsLo() + 1);
        scatterAt(vw, sp, site, cx, cz, h, n, x0, z0, x1, z1, out);
    }

    private static void scatterAt(ValleyWorld vw, PlantHabitat.Entry sp, PlantHabitat.Site site, int cx, int cz, int h, int n,
                                  int x0, int z0, int x1, int z1, List<Clump> out) {
        int r = (int) Math.ceil(1.4 * Math.sqrt(n)) + 1;
        for (int k = 0; k < n; k++) {
            int hk = Noise.hash(h, k, 77);
            int x = cx + Math.floorMod(hk, 2 * r + 1) - r, z = cz + Math.floorMod(hk >>> 12, 2 * r + 1) - r;
            if (x < x0 || x >= x1 || z < z0 || z >= z1) continue;
            String cls = vw.classDef(vw.classAt(x, z)).id();
            if (!PlantHabitat.fitsAny(sp, new PlantHabitat.Site(cls, site.slopeDeg(), site.south(), site.north(), site.startDistM())) && !START_PATCH.contains(sp.id())) continue;
            if (PlantHabitat.NOT_PLACED.contains(cls)) continue;
            out.add(new Clump(x, z, sp.id()));
        }
    }
}
