package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * M2, pure: the layers turned into block columns, the slice cut, coal seams, trees and their shapes, on one seed of
 * each archetype plus the slice test seed (3141592, the GameTest dimension's layer seed). The GameTests check the
 * same things on real blocks in {@code yearofice:slice_test}.
 */
class M2WorldTest {
    static final long[] SEEDS = {0, 1, 2, 3, 3141592};
    private static final Map<Long, ValleyWorld> CACHE = new LinkedHashMap<>();

    static synchronized ValleyWorld world(long seed) {
        return CACHE.computeIfAbsent(seed, s -> ValleyWorld.build(Fixture.PIPE, s));
    }

    // ------------------------------------------------------------------ cover

    @Test
    void coverRulesParseAndTheLastClassTakesEverything() {
        Cover.Params cp = Fixture.PIPE.params().cover();
        assertTrue(cp.classes().size() >= 12, "cover classes: " + Cover.ids(cp));
        Cover.Sample s = new Cover.Sample();
        s.water = 0;
        s.slopeDeg = 20;
        s.aspectDeg = 10;
        s.elevM = 300;
        s.waterDistM = 500;
        s.aboveWaterM = 50;
        s.patch = 0.5;
        assertEquals("spruce_taiga", cp.classes().get(Cover.classify(cp, s)).id());
        List<String> biomes = List.of("open_river", "lake", "gravel_bar", "riparian", "spruce_taiga", "black_spruce_flats", "muskeg",
                "cloudberry_muskeg", "slough_country", "birch_aspen_slope", "foothill_benches", "lichen_woodland", "burn", "alpine_tundra",
                "upland_barrens", "rock_scree", "alpine_range", "icefield", "hot_spring_basin", "tailings");
        for (Cover.ClassDef d : cp.classes()) {
            assertTrue(biomes.contains(d.biome().substring("yearofice:".length())), d.id() + " maps to unknown biome " + d.biome());
        }
    }

    @Test
    void everySeedHasForestRiverAndNoClassSwallowsTheCore() {
        for (long seed : SEEDS) {
            ValleyWorld w = world(seed);
            Cover.Params cp = w.p.cover();
            int[] h = w.cover.histogram(cp.classes().size());
            int total = w.n * w.n;
            for (int i = 0; i < h.length; i++) {
                assertTrue(h[i] < total * 0.5, "seed " + seed + ": " + cp.classes().get(i).id() + " covers " + h[i] * 100 / total + "%");
            }
            assertTrue(h[cp.index("spruce_taiga")] + h[cp.index("spruce_moss")] > total / 10, "seed " + seed + ": little taiga");
            assertTrue(h[cp.index("open_river")] > 0 && h[cp.index("willow_flat")] > 0, "seed " + seed + ": no river or willow flat");
        }
    }

    // ------------------------------------------------------------------ columns

    /**
     * {@code worldgen/no_spill} statically, over 512 × 512 blocks around the start on every seed: every water block
     * has water or solid ground on its four sides and below, except where a falls column is left to run; and no
     * water stands above its cell's stage 4 surface.
     */
    @Test
    void waterNeverMeetsAirAtItsOwnLevel() {
        for (long seed : SEEDS) {
            ValleyWorld w = world(seed);
            int[] s = w.spawn();
            int x0 = s[0] - 256, z0 = s[2] - 256, n = 512;
            ValleyWorld.Columns c = w.columns(x0, z0, n, n);
            int water = 0, lips = 0, flow = 0;
            for (int k = 1; k < n - 1; k++) {
                for (int i = 1; i < n - 1; i++) {
                    int a = k * n + i;
                    if (c.kind[a] == ValleyWorld.LIP) lips++;
                    if (c.kind[a] != ValleyWorld.WATER) continue;
                    water++;
                    int cell = w.cellOf(x0 + i, z0 + k);
                    assertTrue(cell >= 0 && c.water[a] <= (int) Math.floor(w.ws[cell] + 0.5), "seed " + seed + ": water above the surface at " + (x0 + i) + "," + (z0 + k));
                    assertTrue(c.water[a] > c.ground[a], "empty water column");
                    if (c.has(a, ValleyWorld.F_FLOW)) { flow++; continue; }
                    for (int d = 0; d < 4; d++) {
                        int b = (k + ValleyWorld.DZ4[d]) * n + i + ValleyWorld.DX4[d];
                        for (int y = c.ground[a] + 1; y <= c.water[a]; y++) {
                            assertTrue(holds(c, b, y), "seed " + seed + ": water at " + (x0 + i) + " " + y + " " + (z0 + k)
                                    + " meets air beside it (neighbour kind " + c.kind[b] + ", top " + c.top(b) + ")");
                        }
                    }
                }
            }
            assertTrue(water > 1000, "seed " + seed + ": only " + water + " water columns near the start");
            System.out.printf("no_spill seed %d: %d water columns, %d riffle lips, %d falls columns%n", seed, water, lips, flow);
        }
    }

    /** True when block y of column b is solid or water. */
    static boolean holds(ValleyWorld.Columns c, int b, int y) {
        return switch (c.kind[b]) {
            case ValleyWorld.WATER -> y <= c.water[b];
            case ValleyWorld.LIP -> y <= c.lip[b];
            default -> y <= c.ground[b];
        };
    }

    /** A chunk's columns are the same whether asked alone or inside a larger window: no seams between chunks. */
    @Test
    void columnsAgreeAcrossWindows() {
        ValleyWorld w = world(3141592);
        int[] s = w.spawn();
        for (int t = 0; t < 40; t++) {
            int cx = (s[0] >> 4) - 10 + (t * 7) % 21, cz = (s[2] >> 4) - 10 + (t * 13) % 21;
            ValleyWorld.Columns one = w.columns(cx * 16, cz * 16, 16, 16);
            ValleyWorld.Columns big = w.columns(cx * 16 - 16, cz * 16 - 16, 48, 48);
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int a = z * 16 + x, b = big.at(cx * 16 + x, cz * 16 + z);
                assertEquals(big.ground[b], one.ground[a]);
                assertEquals(big.water[b], one.water[a]);
                assertEquals(big.kind[b], one.kind[a]);
                assertEquals(big.lip[b], one.lip[a]);
                assertEquals(big.surface[b], one.surface[a]);
                if (one.kind[a] == ValleyWorld.ROCK) assertEquals(big.shellBottom[b], one.shellBottom[a]);
            }
        }
    }

    /** Explore, don't dig: rock is face rock down to its lowest open side only, nodes only on that shell, two soil blocks. */
    @Test
    void nodesOnlyOnTheShell() {
        for (long seed : SEEDS) {
            ValleyWorld w = world(seed);
            int nodes = 0;
            for (int[] p : w.ironNodes()) {
                ValleyWorld.Columns c = w.columns(p[0], p[1], 1, 1);
                if (!c.has(0, ValleyWorld.F_IRON)) continue;
                nodes++;
                assertEquals(ValleyWorld.ROCK, c.kind[0], "iron on a non-rock column");
            }
            assertTrue(nodes > 0, "seed " + seed + ": no iron node column");
            for (CoalSeams.Seam m : w.seams) {
                ValleyWorld.Columns c = w.columns((int) m.x() - 6, (int) m.z() - 6, 13, 13);
                boolean any = false;
                for (int i = 0; i < 169; i++) {
                    if (!c.has(i, ValleyWorld.F_COAL)) continue;
                    any = true;
                    assertEquals(ValleyWorld.ROCK, c.kind[i]);
                    assertTrue(c.coalY[i] >= c.shellBottom[i] && c.coalY[i] <= c.ground[i], "coal behind the shell");
                }
                assertTrue(any, "seed " + seed + ": seam at " + m.x() + "," + m.z() + " has no coal column");
            }
        }
    }

    // ------------------------------------------------------------------ places

    /** The slice: holds the outpost, the start stands on it, water within 64 blocks, a 2-deep pool at its spot. */
    @Test
    void sliceValleyHoldsTheStartAndTheFeatures() {
        int complete = 0;
        for (long seed : SEEDS) {
            ValleyWorld w = world(seed);
            SliceCut.Window cut = w.slice;
            int[] s = w.spawn();
            int[] r = ValleyWorld.rect(w.outpost());
            assertTrue(cut.contains(s[0], s[2]), "seed " + seed + ": start outside the cut");
            assertTrue(s[0] >= r[0] && s[0] < r[0] + r[2] && s[2] >= r[1] && s[2] < r[1] + r[3], "seed " + seed + ": start off the outpost");
            assertEquals(ValleyWorld.padY(w.outpost()) + 1, s[1], "seed " + seed + ": start not on the pad");
            if (cut.complete()) complete++;
            ValleyWorld.Columns c = w.columns(s[0] - 64, s[2] - 64, 129, 129);
            boolean wet = false;
            for (int i = 0; i < c.kind.length && !wet; i++) wet = c.kind[i] == ValleyWorld.WATER;
            assertTrue(wet, "seed " + seed + ": no water within 64 blocks of the start");
            assertTrue(w.sliceSpot != null && cut.contains(w.sliceSpot.x(), w.sliceSpot.z()), "seed " + seed + ": no spot in the cut");
            int x = (int) Math.floor(w.sliceSpot.x()), z = (int) Math.floor(w.sliceSpot.z());
            ValleyWorld.Columns p = w.columns(x - 2, z - 2, 5, 5);
            int deep = 0;
            for (int i = 0; i < 25; i++) if (p.kind[i] == ValleyWorld.WATER && p.water[i] - p.ground[i] >= 2) deep++;
            assertTrue(deep >= 4, "seed " + seed + ": the spot pool has " + deep + " columns 2 blocks deep");
            System.out.printf("slice seed %d: %s, spot %s q %.2f%n", seed, cut, w.sliceSpot.type(), w.sliceSpot.q());
        }
        assertTrue(complete >= SEEDS.length - 1, complete + " of " + SEEDS.length + " seeds cut a complete slice");
    }

    /** {@code worldgen/coal_seams}: 3–6 seams, at least one 1.5–3 km from the start, none within 600 m. */
    @Test
    void coalSeams() {
        for (long seed : SEEDS) {
            ValleyWorld w = world(seed);
            TerrainParams.Coal cp = w.p.terrain().coal();
            assertTrue(w.seams.size() >= cp.count().min() && w.seams.size() <= cp.count().max(), "seed " + seed + ": " + w.seams.size() + " seams");
            boolean ring = false;
            for (CoalSeams.Seam s : w.seams) {
                assertTrue(s.fromStartM() >= cp.minStartM(), "seed " + seed + ": seam " + s.fromStartM() + " m from the start");
                if (cp.ringM().contains(s.fromStartM())) ring = true;
            }
            assertTrue(ring, "seed " + seed + ": no seam 1.5–3 km out");
        }
    }

    // ------------------------------------------------------------------ trees

    /** Trees come out the same for any query window, never on water, rock or a pad, and all four species grow. */
    @Test
    void treesAreLocalAndSane() {
        ValleyWorld w = world(3141592);
        SliceCut.Window cut = w.slice;
        int[] count = new int[4];
        int live = 0, dead = 0;
        for (int t = 0; t < 400; t++) {
            int x0 = cut.minX() + (t % 20) * 48, z0 = cut.minZ() + (t / 20) * 48;
            List<TreePlan.Tree> small = TreePlan.treesIn(w, x0, z0, x0 + 16, z0 + 16);
            List<TreePlan.Tree> big = new ArrayList<>();
            for (TreePlan.Tree tr : TreePlan.treesIn(w, x0 - 16, z0 - 16, x0 + 32, z0 + 32)) {
                if (tr.x() >= x0 && tr.x() < x0 + 16 && tr.z() >= z0 && tr.z() < z0 + 16) big.add(tr);
            }
            assertEquals(big.size(), small.size(), "trees differ by window at " + x0 + "," + z0);
            for (int i = 0; i < small.size(); i++) assertEquals(big.get(i), small.get(i));
            for (TreePlan.Tree tr : small) {
                ValleyWorld.Columns c = w.columns(tr.x(), tr.z(), 1, 1);
                assertTrue(c.kind[0] == ValleyWorld.SOIL, "tree on " + c.kind[0] + " at " + tr.x() + "," + tr.z());
                count[tr.species()]++;
                if (tr.kind() == TreePlan.Kind.LIVE) live++; else dead++;
            }
        }
        for (int s = 0; s < 4; s++) assertTrue(count[s] > 0, "no " + Cover.SPECIES.get(s) + " in the slice sample");
        assertTrue(dead > 0 && dead < live / 5, dead + " deadwood to " + live + " live");
        System.out.printf("trees in 400 slice chunks: white spruce %d, black spruce %d, birch %d, willow %d; %d deadwood%n",
                count[0], count[1], count[2], count[3], dead);
    }

    /** Shapes stay near their base (inside a feature's one-chunk write margin) and stand on their trunk. */
    @Test
    void treeShapesStayInReach() {
        for (int sp = 0; sp < 4; sp++) {
            for (TreePlan.Kind k : TreePlan.Kind.values()) {
                for (int h0 = 2; h0 <= 24; h0 += 3) {
                    final int h = h0;
                    TreePlan.Tree t = new TreePlan.Tree(0, 0, sp, h, k, sp == Cover.BLACK_SPRUCE ? 1 : -1, 1234567 * h + sp);
                    int[] reach = {0};
                    boolean[] base = {false};
                    TreeShapes.build(t, (dx, dy, dz, part) -> {
                        reach[0] = Math.max(reach[0], Math.max(Math.abs(dx), Math.abs(dz)));
                        assertTrue(dy >= 0 && dy <= h + 2, "part at height " + dy);
                        if (dx == 0 && dz == 0 && dy == 0) base[0] = true;
                    });
                    assertTrue(reach[0] <= 12, sp + " " + k + " reaches " + reach[0]);
                    assertTrue(base[0], sp + " " + k + " h" + h + " has no base block");
                }
            }
        }
    }
}
