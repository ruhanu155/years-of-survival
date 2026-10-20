package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * M3 lane A, pure: the 12 start checks, the score S and the 20-attempt reroll (Blueprint › Start site). The 50-seed
 * test runs the whole search and then re-measures each winner's checks on the final world with code that shares
 * nothing with {@link StartSite#evaluate}.
 */
class StartSiteTest {
    static final StartSiteParams SP = StartSiteParams.load();
    /** The full 50-seed claim is opt-in (./gradlew test -Pstartsite.full, about 20 min); the default build samples a few seeds. */
    static final boolean FULL = Boolean.getBoolean("startsite.full");
    static final int SEEDS = FULL ? 50 : 1;
    /** Seed 1 passes on its first attempt, so the default sample costs one pipeline run. */
    static final long FIRST = FULL ? 0 : 1;

    // ------------------------------------------------------------------ the score

    @Test
    void scoreTermsFollowTheBlueprintFormula() {
        // S = min(1, trees / 80) + 0.5 min(1, birch / 10) + 0.5 (1 - d_face / 350) + 0.5 f + 0.3 a + 0.4 v
        double[] best = StartSite.terms(SP, 200, 40, 0, 1, 1, 1);
        assertEquals(1 + 0.5 + 0.5 + 0.5 + 0.3 + 0.4, StartSite.sum(best), 1e-12);
        double[] mid = StartSite.terms(SP, 40, 5, 175, 0.5, 0.5, 0.7);
        assertEquals(0.5 + 0.25 + 0.25 + 0.25 + 0.15 + 0.28, StartSite.sum(mid), 1e-12);
        assertEquals(0.0, StartSite.sum(StartSite.terms(SP, 0, 0, 350, 0, 0, 0)), 1e-12);
    }

    @Test
    void floodTermIsOneAtTwoToFourMetresHalfAtFourToSixAndZeroOtherwise() {
        StartSiteParams.Score s = SP.score();
        assertEquals(1.0, StartSite.floodTerm(s, 2.0));
        assertEquals(1.0, StartSite.floodTerm(s, 4.0));
        assertEquals(0.5, StartSite.floodTerm(s, 4.01));
        assertEquals(0.5, StartSite.floodTerm(s, 6.0));
        assertEquals(0.0, StartSite.floodTerm(s, 6.01));
        assertEquals(0.0, StartSite.floodTerm(s, 1.99));
        assertEquals(0.0, StartSite.floodTerm(s, Double.NaN));
    }

    @Test
    void aspectTermRunsFromNorthFacingToSouthFacing() {
        // Downhill is the way a slope faces: bearing 0 is north (dz < 0), 180 is south.
        assertEquals(0.0, StartSite.aspectTerm(0, -0.2), 1e-12);
        assertEquals(1.0, StartSite.aspectTerm(0, 0.2), 1e-12);
        assertEquals(0.5, StartSite.aspectTerm(0.2, 0), 1e-12);
        assertEquals(0.5, StartSite.aspectTerm(0.0001, 0.0001), 1e-12, "flat ground has no side");
    }

    @Test
    void paramsAreStrictAndCarryTheBlueprintNumbers() {
        assertEquals(60, SP.waterMaxM());
        assertEquals(8, SP.flatMaxSlopeDeg());
        assertEquals(40, SP.treesMin());
        assertEquals(2, SP.deadwoodMin());
        assertEquals(3, SP.birchMin());
        assertEquals(0.5, SP.fishMinQ());
        assertEquals(0.8, SP.fishNoneAtOrAboveQ());
        assertEquals(400, SP.ironMinM());
        assertEquals(1000, SP.ironMaxM());
        assertEquals(1500, SP.setPieceWithinM());
        assertEquals(500, SP.denClearM());
        assertEquals(1200, SP.mooseWithinM());
        assertEquals(12, StartSite.CHECKS.size());
    }

    // ------------------------------------------------------------------ the search

    /** What a seed keeps once its layers are dropped: the reports are small, the layers are not. */
    private record Seed(long level, int attempts, int rerolls, List<StartSite.Report> reports, List<String> problems) {}

    @Test
    void fiftySeedsPassPlacementWithinTwentyRerollsWithAPerCheckReport() throws Exception {
        Pipeline pipe = Fixture.PIPE;
        ExecutorService pool = Executors.newFixedThreadPool(1);
        List<Future<Seed>> jobs = new ArrayList<>();
        for (long s = FIRST; s < FIRST + SEEDS; s++) {
            long seed = s;
            jobs.add(pool.submit(() -> {
                StartSite.Outcome o = StartSite.search(pipe, seed, SP);
                ValleyWorld w = new ValleyWorld(pipe.params(), o.result(), o.cover(), pipe.setPieces(), o.placements(), true);
                return new Seed(seed, o.reports().size(), o.rerolls(), o.reports(), verify(pipe, w, o));
            }));
        }
        pool.shutdown();
        int rerolls = 0, worst = 0;
        List<String> problems = new ArrayList<>();
        StringBuilder blockers = new StringBuilder();
        java.util.Map<String, Integer> byCheck = new java.util.TreeMap<>();
        for (Future<Seed> f : jobs) {
            Seed s = f.get();
            assertTrue(s.attempts() <= pipe.params().seed().maxAttempts(), "seed " + s.level() + ": " + s.attempts() + " attempts");
            assertTrue(s.reports().get(s.attempts() - 1).passed(), "seed " + s.level() + ": last attempt did not pass");
            for (StartSite.Report r : s.reports()) {
                if (r.passed()) continue;
                // The report names what failed: a stage check, or the first of the 12 checks with nothing left.
                String b = r.blocker();
                assertNotNull(b, "seed " + s.level() + " attempt " + r.attempt() + " has no blocker");
                if (r.stageFailures().isEmpty()) {
                    assertTrue(StartSite.CHECKS.contains(b) || b.equals("no candidate pad"), "seed " + s.level() + ": blocker " + b);
                    assertEquals(12, r.funnel().size());
                    byCheck.merge(b, 1, Integer::sum);
                } else {
                    byCheck.merge("stage", 1, Integer::sum);
                }
            }
            rerolls += s.rerolls();
            worst = Math.max(worst, s.rerolls());
            for (String p : s.problems()) problems.add("seed " + s.level() + ": " + p);
            blockers.append(s.level()).append(':').append(s.rerolls()).append(' ');
        }
        System.out.println("start site: " + SEEDS + " seeds, " + rerolls + " rerolls in all, worst " + worst + "; rerolls per seed " + blockers
                + "\nrerolled at: " + byCheck);
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** Re-measures the winner on the final world, without {@link StartSite#evaluate}. */
    private static List<String> verify(Pipeline pipe, ValleyWorld w, StartSite.Outcome o) {
        List<String> bad = new ArrayList<>();
        SetPieces.Placement q = w.outpost();
        if (q == null) return List.of("no outpost placed");
        double x = q.x(), z = q.z();
        CoreGrid g = w.g;

        // Pad: inside the core, off every other set piece, and the spawn stands in front of the door on it.
        int[] r = ValleyWorld.rect(q);
        for (SetPieces.Placement other : w.placements) {
            if (other == q || other.id().equals(q.id())) continue;
            int[] a = ValleyWorld.rect(other);
            boolean apart = a[0] + a[2] <= r[0] || r[0] + r[2] <= a[0] || a[1] + a[3] <= r[1] || r[1] + r[3] <= a[1];
            if (!apart) bad.add("pad overlaps " + other.id());
        }
        int[] spawn = w.spawn();
        if (spawn[0] < r[0] || spawn[0] >= r[0] + r[2] || spawn[2] < r[1] || spawn[2] >= r[1] + r[3]) bad.add("spawn off the pad");
        if (w.start() == null && o.site() == null) bad.add("no site");

        // 1 water within 60 m.
        double wd = Double.MAX_VALUE;
        for (int dz = -32; dz <= 32; dz++) for (int dx = -32; dx <= 32; dx++) {
            int c = g.cellAt(x - g.originX + dx * g.cell, z - g.originZ + dz * g.cell);
            if (c >= 0 && w.wType[c] != WaterStage.DRY) wd = Math.min(wd, Math.hypot(dx * g.cell, dz * g.cell));
        }
        if (wd > SP.waterMaxM() + g.cell) bad.add("water " + wd + " m");

        // 4–5 firewood and tinder, from the final tree plan.
        int live = 0, dead = 0, birch = 0;
        for (TreePlan.Tree t : TreePlan.treesIn(w, (int) x - 151, (int) z - 151, (int) x + 151, (int) z + 151)) {
            double d = Math.hypot(t.x() + 0.5 - x, t.z() + 0.5 - z);
            if (d <= SP.firewoodRadiusM()) { if (t.kind() == TreePlan.Kind.LIVE) live++; else dead++; }
            if (d <= SP.tinderRadiusM() && t.kind() == TreePlan.Kind.LIVE && t.species() == Cover.BIRCH) birch++;
        }
        if (live < SP.treesMin()) bad.add(live + " trees");
        if (dead < SP.deadwoodMin()) bad.add(dead + " deadwood");
        if (birch < SP.birchMin()) bad.add(birch + " birch");

        // 3 shelter: a face over 6 m within 350 m.
        boolean[] faces = StartSite.tallFaceCells(w, o.result(), SP);
        double fd = Double.MAX_VALUE;
        for (int c = 0; c < faces.length; c++) if (faces[c]) fd = Math.min(fd, Math.hypot(g.worldX(c) - x, g.worldZ(c) - z));
        if (fd > SP.faceWithinM() + g.cell) bad.add("face " + fd + " m");

        // 6 fishing: a spot of 0.5+ within 500 m, none of 0.8+ within 150 m.
        double good = Double.MAX_VALUE, great = Double.MAX_VALUE;
        for (WaterStage.Spot s : w.spots) {
            double d = Math.hypot(s.x() - x, s.z() - z);
            if (s.q() >= SP.fishMinQ()) good = Math.min(good, d);
            if (s.q() >= SP.fishNoneAtOrAboveQ()) great = Math.min(great, d);
        }
        if (good > SP.fishWithinM()) bad.add("no spot within " + SP.fishWithinM() + " m (" + good + ")");
        if (great <= SP.fishNoneWithinM()) bad.add("a 0.8 spot at " + great + " m");

        // 8 iron: the nearest outcrop left on the node layer is 400–1,000 m away.
        double iron = Double.MAX_VALUE;
        for (int[] n : w.ironNodes()) iron = Math.min(iron, Math.hypot(n[0] + 0.5 - x, n[1] + 0.5 - z));
        if (iron < SP.ironMinM() - g.cell || iron > SP.ironMaxM() + g.cell) bad.add("iron " + iron + " m");

        // 10 set piece within 1,500 m.
        double sp = Double.MAX_VALUE;
        for (SetPieces.Placement other : w.placements) if (!other.id().equals(q.id())) sp = Math.min(sp, Math.hypot(other.x() - x, other.z() - z));
        if (sp > SP.setPieceWithinM()) bad.add("set piece " + sp + " m");

        // The score the winner carries is the formula over its own terms.
        StartSite.Site s = o.site();
        if (Math.abs(StartSite.sum(s.terms()) - s.score()) > 1e-9) bad.add("score is not the sum of its terms");
        if (s.sosReturn() != null) {
            double sd = Math.hypot(s.sosReturn()[0] - x, s.sosReturn()[2] - z);
            if (sd > SP.sosReturn().searchM() + 2 * g.cell) bad.add("sos_return " + sd + " m from the outpost");
        }
        return bad;
    }

    @Test
    void theSearchIsDeterministicAndHoldsAHardGate() {
        org.junit.jupiter.api.Assumptions.assumeTrue(FULL, "two more full searches; runs with -Pstartsite.full");
        StartSite.Outcome a = StartSite.search(Fixture.PIPE, 7, SP), b = StartSite.search(Fixture.PIPE, 7, SP);
        assertEquals(a.site().hash(), b.site().hash());
        assertEquals(a.reports().size(), b.reports().size());
        assertEquals(a.result().m1Hash(), Fixture.PIPE.attempt(7, a.result().attempt()).m1Hash(),
                "the start-site search leaves stages 1–5 as the pipeline made them");
        assertFalse(a.lines().isEmpty());
    }

    @Test
    void aSeedThatFailsEveryAttemptReportsEachBlocker() throws Exception {
        // A start the 12 checks can never meet (no site within 1 mm of water) fails all twenty and says why.
        String txt = new String(StartSiteParams.class.getClassLoader().getResourceAsStream(StartSiteParams.RESOURCE).readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        StartSiteParams impossible = StartSiteParams.from(GenJson.parse(txt.replace("\"water\": { \"max_dist_m\": 60 }",
                "\"water\": { \"max_dist_m\": 0.001 }"), "impossible"));
        Pipeline pipe = Fixture.PIPE;
        SetPieceDef def = StartSite.outpostDef(pipe.setPieces());
        Pipeline.Result r = pipe.attempt(0, 0);
        List<SetPieces.Placement> bare = new ArrayList<>();
        for (SetPieces.Placement p : r.places().placements()) if (!p.id().equals(def.id())) bare.add(p);
        Cover.Layers cover = Cover.run(r.seed(), r.landform(), r.erosion(), r.water(), pipe.params());
        ValleyWorld probe = new ValleyWorld(pipe.params(), r, cover, pipe.setPieces(), bare, false);
        StartSite.Report rep = StartSite.evaluate(0, r, probe, Ranges.standIn(probe, impossible.life()), impossible, def);
        assertFalse(rep.passed());
        assertEquals("water", rep.blocker());
        assertEquals(0, rep.funnel().get(0).passed());
        assertTrue(rep.funnel().get(0).reached() > 0);
        assertTrue(rep.lines().get(0).contains("nothing left at water"), rep.lines().get(0));
    }
}
