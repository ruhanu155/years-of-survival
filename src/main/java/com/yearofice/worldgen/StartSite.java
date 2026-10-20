package com.yearofice.worldgen;

import com.yearofice.core.hash.StageHash;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The start-site search (Blueprint › Start site): the 12 checks and the score S as pure functions over the
 * stage 1–6 layers, the winning cell, and the 20-attempt reroll with a per-check report. No Minecraft imports, no
 * clock and no shared state, so every JVM picks the same cell for the same level seed.
 *
 * <p><b>Flow.</b> {@link #search} runs the pipeline attempt by attempt (attempt k + 1 on any failed check in
 * stages 1–7, one budget of {@code seed.max_attempts}). A passing attempt gets a probe {@link ValleyWorld} (no
 * outpost yet, so its tree plan has no phantom pad), the stand-in {@link Ranges}, and {@link #evaluate}. The
 * winner becomes the outpost's {@link SetPieces.Placement}, replacing stage 5's provisional pick in a second
 * {@code ValleyWorld}; stages 1–5 and their hashes are untouched.
 *
 * <p><b>Candidates.</b> Every dry cell of an 8 m lattice (all inside the core's edge margin) whose 12 × 12 pad
 * is dry, within the outpost's mean-slope fit, above the river by the set piece's {@code above_river_m} and
 * spaced from the other set pieces. The checks then run in the blueprint's order and the report counts how many
 * candidates reach and pass each ({@link Funnel}); an attempt fails at the first check with nothing left.
 */
public final class StartSite {
    private StartSite() {}


    /** The 12 checks in the blueprint's order. */
    public static final List<String> CHECKS = List.of("water", "flat", "shelter", "firewood", "tinder", "fishing", "trail",
            "iron", "landmark", "set_piece", "bear", "moose");

    /** Candidates that reached a check, passed it (and every check before it), and would pass it on its own. */
    public record Funnel(String check, int reached, int passed, int alone) {}

    /**
     * The chosen cell: its centre in world metres, pad height, score and the score's terms
     * ({@code trees, birch, face, flood, aspect, view}), the measured value of each check as text, the landmark the
     * door turns to, and the gravel-bar return block ({@code sosReturn}: x, y, z, or null if the pad is all there is).
     */
    public record Site(int cell, double x, double z, double y, double score, double[] terms, List<String> measures,
                       String landmarkKind, String landmarkSource, double landmarkX, double landmarkZ, double landmarkDistM,
                       double facingDeg, int rotation, int region, int[] sosReturn) {
        public int hash() {
            return StageHash.of(String.format(Locale.ROOT, "%d,%.3f,%.3f,%.1f,%.6f,%d,%s", cell, x, z, y, score, rotation,
                    sosReturn == null ? "-" : sosReturn[0] + "/" + sosReturn[1] + "/" + sosReturn[2]));
        }
    }

    /** One attempt: {@code stageFailures} when stages 1–6 failed, else the funnel and (if it passed) the site. */
    public record Report(int attempt, List<String> stageFailures, int candidates, List<Funnel> funnel, String failing, Site site) {
        public boolean passed() { return site != null; }

        /** The first check with nothing left, a stage failure's first line, or null when it passed. */
        public String blocker() { return site != null ? null : (failing != null ? failing : stageFailures.get(0)); }

        public List<String> lines() {
            List<String> out = new ArrayList<>();
            if (!stageFailures.isEmpty()) {
                out.add("attempt " + attempt + ": stage check failed: " + String.join("; ", stageFailures));
                return out;
            }
            out.add(String.format(Locale.ROOT, "attempt %d: %s, %d candidates%s", attempt, site != null ? "PASSED" : "FAILED",
                    candidates, failing == null ? "" : " (nothing left at " + failing + ")"));
            for (int i = 0; i < funnel.size(); i++) {
                Funnel f = funnel.get(i);
                String m = site != null ? "  " + site.measures().get(i) : "";
                out.add(String.format(Locale.ROOT, "  %-10s %5d -> %5d (alone %5d)%s", f.check(), f.reached(), f.passed(), f.alone(), m));
            }
            if (site != null) {
                out.add(String.format(Locale.ROOT,
                        "  S %.3f = trees %.2f + birch %.2f + face %.2f + flood %.2f + aspect %.2f + view %.2f | door %s %.0f m away, %.0f deg",
                        site.score(), site.terms()[0], site.terms()[1], site.terms()[2], site.terms()[3], site.terms()[4],
                        site.terms()[5], site.landmarkKind(), site.landmarkDistM(), site.facingDeg()));
            }
            return out;
        }
    }

    /** What {@link #search} found: the layers of the winning attempt, the placements with the outpost, every attempt's report. */
    public record Outcome(Pipeline.Result result, Cover.Layers cover, List<SetPieces.Placement> placements, Site site,
                          List<Report> reports) {
        public int rerolls() { return reports.size() - 1; }

        public List<String> lines() {
            List<String> out = new ArrayList<>();
            out.add(String.format(Locale.ROOT, "Start site: attempt %d won, %d rerolled", result.attempt(), rerolls()));
            for (Report r : reports) if (!r.passed()) out.add(r.attempt() + ": " + r.blocker());
            out.addAll(reports.get(reports.size() - 1).lines());
            return out;
        }
    }

    // ------------------------------------------------------------------------------------------------ search

    /** Runs attempts until the layers and the start site both pass; fails after {@code seed.max_attempts}. */
    public static Outcome search(Pipeline pipe, long levelSeed, StartSiteParams sp) {
        List<Report> reports = new ArrayList<>();
        int max = pipe.params().seed().maxAttempts();
        SetPieceDef def = outpostDef(pipe.setPieces());
        for (int k = 0; k < max; k++) {
            Pipeline.Result r = pipe.attempt(levelSeed, k);
            if (!r.passed()) {
                reports.add(new Report(k, r.failures(), 0, List.of(), null, null));
                continue;
            }
            Cover.Layers cover = Cover.run(r.seed(), r.landform(), r.erosion(), r.water(), pipe.params());
            List<SetPieces.Placement> bare = new ArrayList<>();
            for (SetPieces.Placement q : r.places().placements()) if (!q.id().equals(def.id())) bare.add(q);
            ValleyWorld probe = new ValleyWorld(pipe.params(), r, cover, pipe.setPieces(), List.copyOf(bare), false);
            Report rep = evaluate(k, r, probe, Ranges.standIn(probe, sp.life()), sp, def);
            reports.add(rep);
            if (!rep.passed()) continue;
            List<SetPieces.Placement> all = new ArrayList<>();
            boolean put = false;
            for (SetPieces.Placement q : r.places().placements()) {
                if (q.id().equals(def.id())) { all.add(placement(rep.site(), def, probe, q.relaxed())); put = true; }
                else all.add(q);
            }
            if (!put) all.add(0, placement(rep.site(), def, probe, 0));
            return new Outcome(r, cover, List.copyOf(all), rep.site(), List.copyOf(reports));
        }
        StringBuilder b = new StringBuilder("World generation failed after " + max + " attempts (level seed " + levelSeed + ")");
        for (Report r : reports) b.append("\n  attempt ").append(r.attempt()).append(": ").append(r.blocker()).append(r.funnel().isEmpty() ? "" : " " + r.candidates() + " candidates " + r.funnel());
        throw new IllegalStateException(b.toString());
    }

    static SetPieceDef outpostDef(List<SetPieceDef> defs) {
        for (SetPieceDef d : defs) if (d.id().equals("yearofice:outpost")) return d;
        throw new IllegalStateException("no yearofice:outpost set piece");
    }

    static SetPieces.Placement placement(Site s, SetPieceDef def, ValleyWorld vw, int relaxed) {
        SetPieceDef.Variant v = def.variants().get(0);
        int fx = s.rotation() % 2 == 1 ? v.footprintZ() : v.footprintX(), fz = s.rotation() % 2 == 1 ? v.footprintX() : v.footprintZ();
        return new SetPieces.Placement(def.id(), 0, v.name(), "yearofice:setpiece/" + v.template(), "core", "riverbank", s.x(), s.z(),
                s.y(), s.rotation(), Math.round(s.facingDeg() * 10) / 10.0, fx, fz, false, s.region(),
                Math.round(s.score() * 1000) / 1000.0, relaxed, -1, -1, 0);
    }

    // ------------------------------------------------------------------------------------------------ score

    /** The flood term f: 1 with the floor 2–4 m over summer water, 0.5 at 4–6 m, else 0. */
    public static double floodTerm(StartSiteParams.Score s, double aboveWaterM) {
        if (Double.isNaN(aboveWaterM)) return 0;
        if (aboveWaterM >= s.floodLowMinM() && aboveWaterM <= s.floodLowMaxM()) return 1;
        if (aboveWaterM > s.floodLowMaxM() && aboveWaterM <= s.floodHighMaxM()) return s.floodHighValue();
        return 0;
    }

    /** The aspect term a: 0 facing north to 1 facing south; 0.5 on ground too flat to have a side. */
    public static double aspectTerm(double downhillX, double downhillZ) {
        if (Math.sqrt(downhillX * downhillX + downhillZ * downhillZ) < 1e-3) return 0.5;
        double b = CoreGrid.bearing(downhillX, downhillZ) * (StrictMath.PI / 180.0);
        return 0.5 * (1 - StrictMath.cos(b));
    }

    /** S = min(1, trees / 80) + 0.5 min(1, birch / 10) + 0.5 (1 - d_face / 350) + 0.5 f + 0.3 a + 0.4 v, as its six terms. */
    public static double[] terms(StartSiteParams sp, int trees, int birch, double faceDistM, double flood, double aspect, double view) {
        StartSiteParams.Score s = sp.score();
        return new double[] {Math.min(1, trees / s.treesCap()), s.weightBirch() * Math.min(1, birch / s.birchCap()),
                s.weightFace() * (1 - faceDistM / sp.faceWithinM()), s.weightFlood() * flood, s.weightAspect() * aspect,
                s.weightView() * view};
    }

    static double sum(double[] t) {
        double s = 0;
        for (double v : t) s += v;
        return s;
    }

    // --------------------------------------------------------------------------------------------- evaluate

    /** Trees in 32 m blocks, built on first ask: any area's count is the same whoever asks first. */
    static final class TreeIndex {
        private static final int BLOCK = 32;
        private final ValleyWorld vw;
        private final Map<Long, int[]> blocks = new LinkedHashMap<>();

        TreeIndex(ValleyWorld vw) { this.vw = vw; }

        /** x, z, kind (0 live, 1 deadwood), birch (0/1), four ints a tree. */
        private int[] block(int bx, int bz) {
            return blocks.computeIfAbsent(((long) bx << 32) ^ (bz & 0xFFFFFFFFL), key -> {
                List<TreePlan.Tree> ts = TreePlan.treesIn(vw, bx * BLOCK, bz * BLOCK, bx * BLOCK + BLOCK, bz * BLOCK + BLOCK);
                int[] a = new int[ts.size() * 4];
                for (int i = 0; i < ts.size(); i++) {
                    TreePlan.Tree t = ts.get(i);
                    a[i * 4] = t.x();
                    a[i * 4 + 1] = t.z();
                    a[i * 4 + 2] = t.kind() == TreePlan.Kind.LIVE ? 0 : 1;
                    a[i * 4 + 3] = (t.species() == Cover.BIRCH ? 1 : 0) | (t.kind() == TreePlan.Kind.LIVE && t.species() != Cover.WILLOW ? 2 : 0) | (t.kind() == TreePlan.Kind.LIVE && t.species() == Cover.BIRCH ? 4 : 0);
                }
                return a;
            });
        }

        /** {live trees within rTrees, deadwood within rTrees, live birch within rBirch}, ignoring the pad rectangle. */
        int[] count(double cx, double cz, double rTrees, double rBirch, double rWood, int[] pad) {
            double rMax = Math.max(Math.max(rTrees, rBirch), rWood);
            int b0x = Math.floorDiv((int) Math.floor(cx - rMax), BLOCK), b1x = Math.floorDiv((int) Math.floor(cx + rMax), BLOCK);
            int b0z = Math.floorDiv((int) Math.floor(cz - rMax), BLOCK), b1z = Math.floorDiv((int) Math.floor(cz + rMax), BLOCK);
            int live = 0, dead = 0, birch = 0, wood = 0;
            for (int bz = b0z; bz <= b1z; bz++) for (int bx = b0x; bx <= b1x; bx++) {
                int[] a = block(bx, bz);
                for (int i = 0; i < a.length; i += 4) {
                    int x = a[i], z = a[i + 1];
                    if (x >= pad[0] && z >= pad[1] && x < pad[2] && z < pad[3]) continue;
                    double dx = x + 0.5 - cx, dz = z + 0.5 - cz, d2 = dx * dx + dz * dz;
                    if (d2 <= rTrees * rTrees) { if (a[i + 2] == 0) live++; else dead++; }
                    if ((a[i + 3] & 4) != 0 && d2 <= rBirch * rBirch) birch++;
                    if ((a[i + 3] & 2) != 0 && d2 <= rWood * rWood) wood++;
                }
            }
            return new int[] {live, dead, birch, wood};
        }
    }

    /** One attempt's candidates, funnel and winner. Pure over the layers; the probe world has no outpost. */
    static Report evaluate(int attempt, Pipeline.Result r, ValleyWorld vw, Ranges.Layers life, StartSiteParams sp, SetPieceDef def) {
        CoreGrid g = vw.g;
        int n = g.n, total = n * n;
        WorldgenParams p = vw.p;
        HydroParams.LandmarkParams lm = p.hydro().landmarks();
        SetPieceDef.Variant variant = def.variants().get(0);
        // The door turns to the landmark after the site is chosen, so every pad test uses the square that holds either turn.
        int fx = Math.max(variant.footprintX(), variant.footprintZ()), fz = fx;

        boolean[] wet = new boolean[total];
        for (int i = 0; i < total; i++) wet[i] = vw.wType[i] != WaterStage.DRY;
        float[] dWater = g.distance(wet);
        float[] level = SetPieces.nearestLevel(g, r.water(), wet);
        float[] slopeTan = g.slopes(vw.ground);
        int flatR = (int) Math.ceil(sp.flatAreaM() / (2 * g.cell));
        float[] flatMax = SetPieces.maxFilter(slopeTan, n, flatR);
        double flatCap = CoreGrid.tanDeg(sp.flatMaxSlopeDeg()), padCap = CoreGrid.tanDeg(p.hydro().places().maxSlopeDeg());
        float[] dFace = g.distance(tallFaceCells(vw, r, sp));
        HeightField field = new HeightField(p, g, vw.ground, r.landform());
        TreeIndex trees = new TreeIndex(vw);

        List<double[]> iron = new ArrayList<>();
        for (WaterStage.IronCluster c : r.water().iron()) {
            for (int cell : c.nodes()) iron.add(new double[] {g.worldX(cell), g.worldZ(cell), vw.ground[cell]});
        }
        List<Landmarks.Landmark> landmarks = r.places().landmarks();

        double halfPad = Math.max(fx, fz) / 2.0;
        double minWater = halfPad * Math.sqrt(2) + sp.padClearanceM();
        int step = Math.max(1, (int) Math.round(sp.stepM() / g.cell));
        int margin = (int) Math.ceil(sp.edgeMarginM() / g.cell);
        double skirt = p.terrain().footprintSkirtM() + 1;

        boolean[] isBar = new boolean[vw.p.cover().classes().size()];
        for (int q = 0; q < isBar.length; q++) isBar[q] = sp.site().barClasses().contains(vw.classDef(q).id());
        int nc = CHECKS.size();
        int[] reached = new int[nc], passed = new int[nc], alone = new int[nc];
        int candidates = 0;
        Site best = null;
        int bestTie = 0;
        for (int k = margin; k < n - margin; k += step) for (int i = margin; i < n - margin; i += step) {
            int c = g.idx(i, k);
            if (wet[c] || dWater[c] < minWater) continue;
            double x = g.worldX(c), z = g.worldZ(c);
            if (!placeable(vw, def, g, c, x, z, fx, fz, slopeTan, padCap, level)) continue;
            if (!barClear(vw, g, c, fx, fz, sp.site(), isBar)) continue;
            candidates++;
            boolean[] ok = new boolean[nc];
            String[] m = new String[nc];
            int px = (int) Math.floor(x - fx / 2.0), pz = (int) Math.floor(z - fz / 2.0);
            int[] pad = {(int) Math.ceil(px - skirt), (int) Math.ceil(pz - skirt), (int) Math.ceil(px + fx + skirt), (int) Math.ceil(pz + fz + skirt)};

            double bankM = Float.isNaN(level[c]) ? Double.NaN : vw.ground[c] - level[c];
            ok[0] = dWater[c] <= sp.waterMaxM() && !Double.isNaN(bankM) && bankM >= sp.site().aboveRiverMinM() && bankM <= sp.site().aboveRiverMaxM();
            m[0] = String.format(Locale.ROOT, "water %.0f m, bank %.1f m up", dWater[c], bankM);
            ok[1] = flatMax[c] < flatCap;
            m[1] = String.format(Locale.ROOT, "steepest %.1f deg in %.0f m", CoreGrid.deg(flatMax[c]), sp.flatAreaM());
            double face = dFace[c];
            ok[2] = face <= sp.faceWithinM();
            m[2] = String.format(Locale.ROOT, "face %.0f m", face);
            int[] tc = trees.count(x, z, sp.firewoodRadiusM(), sp.tinderRadiusM(), sp.site().woodWithinM(), pad);
            ok[3] = tc[0] >= sp.treesMin() && tc[1] >= sp.deadwoodMin() && tc[3] >= sp.site().woodMin();
            m[3] = tc[0] + " trees, " + tc[1] + " deadwood, " + tc[3] + " spruce or birch in " + (int) sp.site().woodWithinM() + " m";
            ok[4] = tc[2] >= sp.birchMin();
            m[4] = tc[2] + " birch";
            double nearGood = Double.MAX_VALUE, nearBest = Double.MAX_VALUE;
            for (WaterStage.Spot s : vw.spots) {
                double d = Math.sqrt((s.x() - x) * (s.x() - x) + (s.z() - z) * (s.z() - z));
                if (s.q() >= sp.fishMinQ()) nearGood = Math.min(nearGood, d);
                if (s.q() >= sp.fishNoneAtOrAboveQ()) nearBest = Math.min(nearBest, d);
            }
            ok[5] = nearGood <= sp.fishWithinM() && nearBest > sp.fishNoneWithinM();
            m[5] = String.format(Locale.ROOT, "q>=%.1f at %.0f m, q>=%.1f %s", sp.fishMinQ(), nearGood, sp.fishNoneAtOrAboveQ(),
                    nearBest == Double.MAX_VALUE ? "none" : String.format(Locale.ROOT, "at %.0f m", nearBest));
            int trail = life.trailPointsWithin(x, z, sp.trailWithinM());
            ok[6] = trail >= sp.trailMin();
            m[6] = trail + " trail points";
            double[] nearIron = null;
            double ironD = Double.MAX_VALUE;
            for (double[] q : iron) {
                double d = Math.sqrt((q[0] - x) * (q[0] - x) + (q[1] - z) * (q[1] - z));
                if (sp.ironPrune() && d < sp.ironMinM()) continue; // pruned from the node layer once the site is chosen
                if (d < ironD) { ironD = d; nearIron = q; }
            }
            boolean ironSeen = nearIron != null && seen(vw, field, sp, lm, x, z, nearIron[0], nearIron[1], nearIron[2] + sp.ironOutcropM());
            ok[7] = nearIron != null && ironD >= sp.ironMinM() && ironD <= sp.ironMaxM() && !ironSeen;
            m[7] = String.format(Locale.ROOT, "outcrop %.0f m, %s", ironD, ironSeen ? "in sight" : "hidden");
            Landmarks.Landmark target = null;
            double targetD = 0, targetElev = 0;
            boolean targetTower = false;
            String towerPrefix = sp.score().towerSource();
            for (Landmarks.Landmark l : landmarks) {
                double d = Math.sqrt((l.x() - x) * (l.x() - x) + (l.z() - z) * (l.z() - z));
                if (d > sp.landmarkWithinM()) continue;
                double top = l.baseM() + l.heightM();
                if (!field.clear(x, z, lm.eyeM(), l.x(), l.z(), top - lm.topM(), lm.rayStepM(), lm.nearSkipM(), lm.canopyM())) continue;
                double eye = field.at(x, z) + lm.eyeM();
                double elev = CoreGrid.deg((top - eye) / Math.max(1, d));
                boolean tower = l.source().startsWith(towerPrefix) && elev >= sp.score().towerMinElevationDeg();
                // A tower that clears the horizon angle wins; then the nearer landmark.
                if (target == null || (tower && !targetTower) || (tower == targetTower && d < targetD)) {
                    target = l;
                    targetD = d;
                    targetElev = elev;
                    targetTower = tower;
                }
            }
            ok[8] = target != null;
            m[8] = target == null ? "none in sight" : String.format(Locale.ROOT, "%s %.0f m, %.1f deg up", target.kind(), targetD, targetElev);
            double sd = Double.MAX_VALUE;
            for (SetPieces.Placement q : vw.placements) {
                if (q.id().equals(def.id())) continue;
                sd = Math.min(sd, Math.sqrt((q.x() - x) * (q.x() - x) + (q.z() - z) * (q.z() - z)));
            }
            ok[9] = sd <= sp.setPieceWithinM();
            m[9] = String.format(Locale.ROOT, "set piece %.0f m", sd);
            double den = life.nearestDen(x, z);
            int bears = life.bearRangesCovering(x, z);
            ok[10] = den >= sp.denClearM() && bears >= sp.bearRangesMin();
            m[10] = String.format(Locale.ROOT, "den %.0f m, %d bear range", Math.min(den, 99999), bears);
            double mooseD = life.nearestMoose(x, z);
            ok[11] = mooseD <= sp.mooseWithinM();
            m[11] = String.format(Locale.ROOT, "moose range %.0f m", mooseD);

            boolean all = true;
            for (int q = 0; q < nc; q++) {
                if (ok[q]) alone[q]++;
                if (all) reached[q]++;
                all &= ok[q];
                if (all) passed[q]++;
            }
            if (!all) continue;

            double above = Float.isNaN(level[c]) ? Double.NaN : vw.ground[c] - level[c];
            int a = 5;
            double gx = (vw.ground[g.idx(Math.min(n - 1, i + a), k)] - vw.ground[g.idx(Math.max(0, i - a), k)]) / (2 * a * g.cell);
            double gz = (vw.ground[g.idx(i, Math.min(n - 1, k + a))] - vw.ground[g.idx(i, Math.max(0, k - a))]) / (2 * a * g.cell);
            double view = targetTower ? sp.score().viewTowerValue() : sp.score().viewOtherValue();
            double[] t = terms(sp, tc[0], tc[2], face, floodTerm(sp.score(), above), aspectTerm(-gx, -gz), view);
            double score = sum(t);
            int tie = Noise.hash(vw.seed.attemptSeed(), c, 0x57A7);
            if (best == null || score > best.score() + 1e-9 || (Math.abs(score - best.score()) <= 1e-9 && Integer.compareUnsigned(tie, bestTie) < 0)) {
                double bearing = CoreGrid.bearing(target.x() - x, target.z() - z);
                best = new Site(c, x, z, padLevel(g, vw.ground, c, fx, fz), score, t, List.of(m), target.kind(), target.source(),
                        target.x(), target.z(), targetD, Math.round(bearing * 10) / 10.0, SetPieces.rotationFor(bearing), field.region(x, z), null);
                bestTie = tie;
            }
        }
        List<Funnel> funnel = new ArrayList<>();
        String failing = null;
        for (int i = 0; i < nc; i++) {
            funnel.add(new Funnel(CHECKS.get(i), reached[i], passed[i], alone[i]));
            if (failing == null && passed[i] == 0) failing = CHECKS.get(i);
        }
        if (candidates == 0) failing = "no candidate pad";
        if (best != null) best = withSosReturn(best, vw, sp, level);
        return new Report(attempt, List.of(), candidates, List.copyOf(funnel), best == null ? failing : null, best);
    }

    /**
     * The cells of every rock face piece taller than {@code face_min_height_m}: stage 4 keeps each piece's foot, so
     * the piece is its foot's connected rock inside the same {@code tile_m} tile (the rule that made it), and the
     * distance to a face is the distance to the nearest of its cells, not to one point of it.
     */
    static boolean[] tallFaceCells(ValleyWorld vw, Pipeline.Result r, StartSiteParams sp) {
        CoreGrid g = vw.g;
        int n = g.n, tile = (int) (vw.p.hydro().rock().tileM() / g.cell);
        boolean[] out = new boolean[n * n];
        int[] queue = new int[tile * tile];
        for (WaterStage.Face f : r.water().faces()) {
            if (f.heightM() <= sp.faceMinHeightM()) continue;
            int tx = g.col(f.foot()) / tile * tile, ty = g.row(f.foot()) / tile * tile;
            int qh = 0, qt = 0;
            queue[qt++] = f.foot();
            out[f.foot()] = true;
            while (qh < qt) {
                int c = queue[qh++];
                for (int m = 0; m < 8; m++) {
                    int x = g.col(c) + CoreGrid.DX[m], z = g.row(c) + CoreGrid.DZ[m];
                    if (x < tx || z < ty || x >= Math.min(n, tx + tile) || z >= Math.min(n, ty + tile)) continue;
                    int nb = g.idx(x, z);
                    if (out[nb] || (vw.rock[nb] != WaterStage.FACE_ROCK && vw.rock[nb] != WaterStage.IRON_OUTCROP)) continue;
                    out[nb] = true;
                    queue[qt++] = nb;
                }
            }
        }
        return out;
    }

    /**
     * Whether a standing player sees a point: the ground does not hide it, and the line does not run through
     * {@code forest_block_m} of treed cover below {@code forest_height_m} (the taiga hides what it hides).
     */
    static boolean seen(ValleyWorld vw, HeightField field, StartSiteParams sp, HydroParams.LandmarkParams lm, double ax, double az,
                        double bx, double bz, double targetY) {
        if (!field.clear(ax, az, sp.ironEyeM(), bx, bz, targetY, lm.rayStepM(), lm.nearSkipM(), 0)) return false;
        double ay = field.at(ax, az) + sp.ironEyeM();
        double len = Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
        double forest = 0;
        for (double s = lm.nearSkipM(); s < len; s += lm.rayStepM()) {
            double t = s / len, x = ax + (bx - ax) * t, z = az + (bz - az) * t;
            double above = ay + (targetY - ay) * t - field.at(x, z);
            if (above < sp.ironForestHeightM() && !vw.classDef(vw.classAt((int) Math.floor(x), (int) Math.floor(z))).trees().isEmpty()) {
                forest += lm.rayStepM();
                if (forest >= sp.ironForestBlockM()) return false;
            }
        }
        return true;
    }

    /** Bar cover scores 0: none under the pad (plus its margin), and no more than the ring's share of it around. */
    static boolean barClear(ValleyWorld vw, CoreGrid g, int c, int fx, int fz, StartSiteParams.Site st, boolean[] isBar) {
        int ci = g.col(c), ck = g.row(c);
        int padR = (int) Math.ceil((Math.max(fx, fz) / 2.0 + st.padMarginM()) / g.cell);
        int ringR = (int) Math.ceil(st.ringM() / g.cell);
        int bar = 0, all = 0;
        for (int k = Math.max(0, ck - ringR); k <= Math.min(g.n - 1, ck + ringR); k++) for (int i = Math.max(0, ci - ringR); i <= Math.min(g.n - 1, ci + ringR); i++) {
            boolean b = isBar[vw.cover.classAt(g.idx(i, k))];
            if (b && Math.abs(i - ci) <= padR && Math.abs(k - ck) <= padR) return false;
            int di = i - ci, dk = k - ck;
            if (di * di + dk * dk > ringR * ringR) continue;
            all++;
            if (b) bar++;
        }
        return all == 0 || bar <= st.barRingMax() * all;
    }

    /** Dry pad, within the outpost's mean slope and the stage-5 cap, above the river and spaced from the other set pieces. */
    private static boolean placeable(ValleyWorld vw, SetPieceDef def, CoreGrid g, int c, double x, double z, int fx, int fz,
                                     float[] slopeTan, double padCap, float[] level) {
        double lx0 = x - g.originX - fx / 2.0, lz0 = z - g.originZ - fz / 2.0;
        int i0 = (int) Math.floor(lx0 / g.cell), i1 = (int) Math.floor((lx0 + fx - 0.01) / g.cell);
        int k0 = (int) Math.floor(lz0 / g.cell), k1 = (int) Math.floor((lz0 + fz - 0.01) / g.cell);
        if (i0 < 2 || k0 < 2 || i1 > g.n - 3 || k1 > g.n - 3) return false;
        double sum = 0;
        int cnt = 0;
        for (int k = k0; k <= k1; k++) for (int i = i0; i <= i1; i++) {
            int idx = g.idx(i, k);
            if (vw.wType[idx] != WaterStage.DRY || slopeTan[idx] > padCap) return false;
            sum += CoreGrid.deg(slopeTan[idx]);
            cnt++;
        }
        if (sum / cnt > def.fit().maxSlopeDeg()) return false;
        if (!Float.isNaN(level[c]) && vw.ground[c] - level[c] < def.fit().aboveRiverM()) return false;
        for (SetPieces.Placement q : vw.placements) {
            double dx = q.x() - x, dz = q.z() - z;
            double need = Math.max(def.spacingM(), 0.5 * (q.footprintX() + q.footprintZ()) + 8);
            if (dx * dx + dz * dz < need * need) return false;
        }
        return true;
    }

    /** The pad's floor: the median ground under the footprint, to 0.1 m (as stage 5 levels its pads). */
    static double padLevel(CoreGrid g, float[] ground, int c, int fx, int fz) {
        double lx0 = g.lx(c) - fx / 2.0, lz0 = g.lz(c) - fz / 2.0;
        int i0 = (int) Math.floor(lx0 / g.cell), i1 = (int) Math.floor((lx0 + fx - 0.01) / g.cell);
        int k0 = (int) Math.floor(lz0 / g.cell), k1 = (int) Math.floor((lz0 + fz - 0.01) / g.cell);
        float[] hs = new float[(i1 - i0 + 1) * (k1 - k0 + 1)];
        int cnt = 0;
        for (int k = k0; k <= k1; k++) for (int i = i0; i <= i1; i++) hs[cnt++] = ground[g.idx(i, k)];
        java.util.Arrays.sort(hs, 0, cnt);
        return Math.round(hs[cnt / 2] * 10) / 10.0;
    }

    /**
     * {@code yearofice_world.params.sos_return}: a dry gravel-bar block within {@code search_m} of the pad, over
     * summer water and, when one exists, over the flood-year line; the nearest such wins, then the nearest dry
     * bank cell near water. Null if the reach has no bar at all.
     */
    private static Site withSosReturn(Site s, ValleyWorld vw, StartSiteParams sp, float[] level) {
        CoreGrid g = vw.g;
        StartSiteParams.SosReturn o = sp.sosReturn();
        int reach = (int) Math.ceil(o.searchM() / g.cell);
        int ci = g.col(s.cell()), ck = g.row(s.cell());
        int best = -1;
        double bestKey = Double.MAX_VALUE;
        for (int k = Math.max(0, ck - reach); k <= Math.min(g.n - 1, ck + reach); k++) for (int i = Math.max(0, ci - reach); i <= Math.min(g.n - 1, ci + reach); i++) {
            int idx = g.idx(i, k);
            if (vw.bank[idx] != WaterStage.BAR || vw.wType[idx] != WaterStage.DRY || Float.isNaN(level[idx])) continue;
            double above = vw.ground[idx] - level[idx];
            if (above < o.aboveWaterMinM()) continue;
            double d = Math.sqrt((double) (i - ci) * (i - ci) + (double) (k - ck) * (k - ck)) * g.cell;
            if (d > o.searchM()) continue;
            // Over the flood-year line first, then the nearest.
            double key = d + (above >= o.floodYearRiseM() ? 0 : 10000);
            if (key < bestKey) { bestKey = key; best = idx; }
        }
        if (best < 0) return s;
        int[] ret = {(int) Math.floor(g.worldX(best)), (int) Math.round(vw.ground[best]), (int) Math.floor(g.worldZ(best))};
        return new Site(s.cell(), s.x(), s.z(), s.y(), s.score(), s.terms(), s.measures(), s.landmarkKind(), s.landmarkSource(),
                s.landmarkX(), s.landmarkZ(), s.landmarkDistM(), s.facingDeg(), s.rotation(), s.region(), ret);
    }
}
