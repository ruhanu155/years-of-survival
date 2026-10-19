package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything the M1 tests check about one seed, measured once from the full layers (which are then dropped).
 * Each list holds the violations of one rule, so a test asserts it is empty and prints what broke.
 */
record M1Summary(
        int waterHash, int placesHash, int m1Hash, int coarseHash,
        List<String> downhill,
        double entryErrorM, double exitErrorM,
        int channels, double braidRunM, double largestLakeM2, int falls, int minimumFalls, int oxbowLakes,
        int majorSpots, int allSpots, List<String> spotRules,
        int shelters, int flagged, int ironClusters, List<String> ironRules,
        List<String> lakeRules,
        List<String> geometryRules,
        List<String> setPieceRules, List<String> placements,
        int landmarksPassing, List<String> landmarkRules) {

    static M1Summary of(Pipeline.Result r, Pipeline pipe) {
        WaterStage.Layers w = r.water();
        PlacesStage.Layers pl = r.places();
        HydroParams hp = pipe.params().hydro();
        Archetype.Params arch = pipe.archetype(r.seed().archetype());
        float[] eroded = r.erosion().height();
        float[] ground = r.ground();
        CoreGrid g = new CoreGrid(pipe.params(), r.seed().mirror());
        int n = w.cells();

        // ------------------------------------------------ every river flows downhill to the edge or a lake
        List<String> downhill = new ArrayList<>();
        for (Rivers.Line ln : w.main().lines()) {
            for (int i = 1; i < ln.size(); i++) {
                if (ln.ws()[i] > ln.ws()[i - 1] + 1e-9) { downhill.add("main rises at point " + i); break; }
            }
            double ex = ln.x()[ln.size() - 1], ez = ln.z()[ln.size() - 1];
            double edge = Math.min(Math.min(ex, g.size - ex), Math.min(ez, g.size - ez));
            if (edge > 2) downhill.add("main ends " + edge + " m inside the core");
        }
        for (WaterStage.Creek c : w.creeks()) {
            // Walk the receivers from the source: the surface never rises, and the walk ends at the edge, a lake
            // or the main river (which itself reaches the edge).
            int k = c.cells()[0];
            int steps = 0;
            while (true) {
                int rc = w.receiver()[k];
                if (rc < 0) {
                    if (!g.edge(k)) downhill.add("creek " + c.bodyId() + " stops inland at " + k);
                    break;
                }
                int t = w.type()[rc];
                if (t == WaterStage.DRY) { downhill.add("creek " + c.bodyId() + " runs onto dry ground at " + rc); break; }
                if (w.surface()[rc] > w.surface()[k] + 1e-3) {
                    downhill.add(String.format(Locale.ROOT, "creek %d rises %.3f m at %d", c.bodyId(), w.surface()[rc] - w.surface()[k], rc));
                    break;
                }
                if (t == WaterStage.MAIN || t == WaterStage.LAKE || t == WaterStage.OXBOW) break;
                k = rc;
                if (++steps > n * 4) { downhill.add("creek " + c.bodyId() + " loops"); break; }
            }
        }

        // -------------------------------------------------------- the core river meets the coarse net
        Rivers.Line axis = w.main().axis();
        double entryErr = dist(axis.x()[0], axis.z()[0], w.coarse().entry().lx(), w.coarse().entry().lz());
        double exitErr = dist(axis.x()[axis.size() - 1], axis.z()[axis.size() - 1], w.coarse().exit().lx(), w.coarse().exit().lz());

        // ------------------------------------------------------------------------- archetype signatures
        double braidRun = 0;
        if (w.main().lines().size() > 1) {
            // Along the valley, count separate runs of main-river water across each 2 m column near the axis.
            double run = 0;
            for (int i = 0; i < n; i++) {
                double lx = (i + 0.5) * g.cell;
                double az = Double.NaN;
                for (int p = 0; p < axis.size(); p++) if (Math.abs(axis.x()[p] - lx) < 1.5) { az = axis.z()[p]; break; }
                if (Double.isNaN(az)) { run = 0; continue; }
                int k0 = (int) ((az - w.main().beltM()) / g.cell), k1 = (int) ((az + w.main().beltM()) / g.cell);
                int runs = 0;
                boolean in = false;
                for (int k = Math.max(0, k0); k <= Math.min(n - 1, k1); k++) {
                    boolean wet = w.type()[k * n + i] == WaterStage.MAIN;
                    if (wet && !in) runs++;
                    in = wet;
                }
                run = runs >= 3 ? run + g.cell : 0;
                braidRun = Math.max(braidRun, run);
            }
        }
        double largest = 0;
        for (WaterStage.Lake l : w.lakes()) largest = Math.max(largest, l.areaM2());

        // ---------------------------------------------------------------------------- fishing spots
        List<String> spotRules = new ArrayList<>();
        int major = 0;
        List<WaterStage.Spot> spots = r.spots();
        for (WaterStage.Spot s : spots) {
            if (s.major()) major++;
            HydroParams.SpotType t = hp.spots().type(s.type());
            if (s.q() < 0 || s.q() > 1) spotRules.add(s.type() + " q " + s.q());
            double lo = t.band().min() * hp.spots().variation().min() - 1e-3, hi = Math.min(1, t.band().max() * hp.spots().variation().max()) + 1e-3;
            if (s.q() < lo || s.q() > hi) spotRules.add(s.type() + " q " + s.q() + " outside " + lo + "-" + hi);
            for (int k = 0; k < s.stock().length; k++) {
                if (s.stock()[k] != (int) Math.round(s.q() * t.capacity()[k])) spotRules.add(s.type() + " stock " + s.stock()[k]);
            }
            if (w.type()[s.cell()] == WaterStage.DRY) spotRules.add(s.type() + " on dry ground at " + s.cell());
        }

        // ------------------------------------------------------------------------------ rock and iron
        int flagged = 0;
        for (WaterStage.Face f : w.faces()) if (f.rank() > 0) flagged++;
        List<String> ironRules = new ArrayList<>();
        for (WaterStage.IronCluster c : w.iron()) {
            for (int node : c.nodes()) {
                if (w.rock()[node] != WaterStage.IRON_OUTCROP) ironRules.add("node " + node + " is not iron_outcrop");
                if (ground[node] <= hp.rock().ironMinElevM()) ironRules.add("node " + node + " below " + hp.rock().ironMinElevM() + " m");
                if (r.landform().hardnessAt(node) <= hp.rock().minHardness()) ironRules.add("node " + node + " on soft rock");
                if (g.slope(ground, node) <= CoreGrid.tanDeg(hp.rock().minSlopeDeg())) ironRules.add("node " + node + " not on a face");
            }
        }

        // ------------------------------------------------------------------------------------- lakes
        List<String> lakeRules = new ArrayList<>();
        for (WaterStage.Lake l : w.lakes()) {
            if (l.outlets() != 1) lakeRules.add("lake " + l.bodyId() + " has " + l.outlets() + " outlets");
            if (l.maxDepthM() < hp.water().lakeMinDepthM() - 1e-6) lakeRules.add("lake " + l.bodyId() + " only " + l.maxDepthM() + " m deep");
            if (l.areaM2() < hp.water().lakeMinAreaM2()) lakeRules.add("lake " + l.bodyId() + " only " + l.areaM2() + " m2");
        }

        // ---------------------------------------------------------------- widths, depths and current
        List<String> geo = new ArrayList<>();
        HydroParams.Water wp = hp.water();
        int chans = w.main().lines().size();
        for (Rivers.Line ln : w.main().lines()) {
            for (int i = 0; i < ln.size(); i += 50) {
                double wantW = wp.width(ln.a()[i] / chans);
                if (Math.abs(ln.w()[i] - wantW) > 1e-9) geo.add("width " + ln.w()[i] + " != " + wantW);
                if (Math.abs(ln.d()[i] - (0.3 + 0.08 * ln.w()[i])) > 1e-9) geo.add("depth " + ln.d()[i]);
                if (ln.v()[i] < 0.3 - 1e-9 || ln.v()[i] > 3.0 + 1e-9) geo.add("speed " + ln.v()[i]);
            }
            if (chans == 1 && (ln.w()[0] < 12 - 1e-6 || ln.w()[ln.size() - 1] > 25 + 1e-6)) {
                geo.add(String.format(Locale.ROOT, "main river %.1f-%.1f m wide, want 12-25", ln.w()[0], ln.w()[ln.size() - 1]));
            }
        }
        for (int i = 0; i < n * n; i++) {
            int t = w.type()[i];
            if (t == WaterStage.MAIN || t == WaterStage.CREEK) {
                double v = w.speedMS(i);
                if ((w.flowDir()[i] & 0xFF) == WaterStage.STILL && !g.edge(i) && w.receiver()[i] >= 0 && t == WaterStage.CREEK) {
                    geo.add("creek cell " + i + " has no current");
                }
                if (v > 3.0 + 1e-9) geo.add("speed " + v + " at " + i);
                int normal = (w.flood()[i] >> 4) & 15, flood = w.flood()[i] & 15;
                if (t == WaterStage.MAIN && (normal != 8 || flood != 14)) geo.add("main flood lines " + normal + "/" + flood);
                if (t == WaterStage.CREEK && (normal != 2 || flood != 5)) geo.add("creek flood lines " + normal + "/" + flood);
            }
            if (t == WaterStage.LAKE && (w.flowDir()[i] & 0xFF) != WaterStage.STILL) geo.add("lake cell " + i + " flows");
            if (w.carve()[i] < 0) geo.add("negative carve at " + i);
        }
        // The main river is always a swim: its channel is over the 0.8 m wading depth all the way.
        if (chans == 1) for (int i = 0; i < axis.size(); i++) {
            if (axis.d()[i] < wp.wadeMaxDepthM()) { geo.add("main river wadeable at point " + i); break; }
        }
        if (geo.size() > 20) geo = new ArrayList<>(geo.subList(0, 20));

        // ------------------------------------------------------------------------------- set pieces
        List<String> sp = new ArrayList<>(pl.check(pipe.setPieces(), w, ground, g, hp));
        sp.removeIf(s -> s.contains("landmark"));
        List<String> placements = new ArrayList<>();
        double cap = CoreGrid.tanDeg(hp.places().maxSlopeDeg());
        for (SetPieces.Placement q : pl.placements()) {
            placements.add(String.format(Locale.ROOT, "%s/%s on %s%s", q.id().substring(10), q.variant(), q.site(), q.coarse() ? " (coarse)" : ""));
            SetPieceDef d = Landmarks.def(pipe.setPieces(), q.id());
            if (!q.coarse()) {
                // Re-measure the footprint from the layers.
                int fx = q.footprintX(), fz = q.footprintZ();
                double lx0 = q.x() - g.originX - fx / 2.0, lz0 = q.z() - g.originZ - fz / 2.0;
                int i0 = (int) Math.floor(lx0 / g.cell), i1 = (int) Math.floor((lx0 + fx - 0.01) / g.cell);
                int k0 = (int) Math.floor(lz0 / g.cell), k1 = (int) Math.floor((lz0 + fz - 0.01) / g.cell);
                for (int k = k0; k <= k1; k++) for (int i = i0; i <= i1; i++) {
                    int idx = g.idx(i, k);
                    if (w.type()[idx] != WaterStage.DRY) { sp.add(q.id() + " stands in water"); k = k1 + 1; break; }
                    if (g.slope(ground, idx) > cap) { sp.add(q.id() + " stands on a slope over 25 degrees"); k = k1 + 1; break; }
                }
            }
            if (q.relaxed() == 0 && !q.coarse()) {
                if (!d.fit().sites().contains(q.site())) sp.add(q.id() + " on " + q.site());
                if (q.site().equals("gravel_bar") && w.bank()[g.cellAt(q.x() - g.originX, q.z() - g.originZ)] != WaterStage.BAR) {
                    sp.add(q.id() + " is not on a gravel bar");
                }
            }
            // Spacing from every piece placed before it (SetPieces.spaced), unless the rules were relaxed twice.
            int at = pl.placements().indexOf(q);
            for (int j = 0; j < at && q.relaxed() < 2; j++) {
                SetPieces.Placement o = pl.placements().get(j);
                double need = Math.max(d.spacingM(), 0.5 * (o.footprintX() + o.footprintZ()) + 8);
                double dd = dist(q.x(), q.z(), o.x(), o.z());
                if (dd < need - 1e-6) sp.add(String.format(Locale.ROOT, "%s is %.0f m from %s, spacing %.0f", q.id(), dd, o.id(), need));
            }
        }

        // -------------------------------------------------------------------------------- landmarks
        List<String> lm = new ArrayList<>();
        int passing = 0;
        boolean[] seen = new boolean[16];
        for (Landmarks.Landmark l : pl.landmarks()) {
            if (l.visiblePoints() >= hp.landmarks().minPoints()) seen[l.region()] = true;
        }
        for (int reg = 0; reg < 16; reg++) {
            if (seen[reg]) passing++;
            else lm.add("no landmark seen from 2 km in " + SeedStage.regionCode(reg));
        }

        return new M1Summary(r.waterHash(), r.placesHash(), r.m1Hash(), w.coarse().hash(), downhill, entryErr, exitErr,
                chans, braidRun, largest, w.falls().size(), arch.minimumFalls(), w.oxbowLakes(), major, spots.size(),
                spotRules, w.shelters().size(), flagged, w.iron().size(), ironRules, lakeRules, geo, sp, placements,
                passing, lm);
    }

    static double dist(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
