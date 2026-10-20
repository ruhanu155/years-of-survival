package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in home ranges, dens and game trails for the start-site checks (Blueprint › Home ranges at world creation;
 * Game trails; Dens and lodges). Stage 7's real {@code Ranges} and {@code Trails} (animals arrive in M5/M6) will
 * replace {@link #standIn}; the checks read only {@link Layers}, so nothing else changes.
 *
 * <p>The stand-in is deliberately plain and deterministic: range centres are picked greedily on a habitat lattice
 * with a hash jitter, at least one radius apart per species; each bear gets a den on the nearest slope in the den
 * band; one wolf den sits on a sandy bank near water; and each range's trail is a straight line, sampled every
 * {@code trail_step_m}, from its centre to the nearest water. No Minecraft imports, no clock, no shared state.
 */
public final class Ranges {
    private Ranges() {}

    public record Range(String species, double x, double z, double radiusM) {
        public boolean covers(double px, double pz) {
            double dx = px - x, dz = pz - z;
            return dx * dx + dz * dz < radiusM * radiusM;
        }
    }

    public record Den(String kind, double x, double z) {}

    /** {@code trail} holds x, z pairs in world metres. */
    public record Layers(List<Range> moose, List<Range> bears, List<Den> dens, double[] trail) {
        public int trailPointsWithin(double x, double z, double r) {
            int n = 0;
            for (int i = 0; i + 1 < trail.length; i += 2) {
                double dx = trail[i] - x, dz = trail[i + 1] - z;
                if (dx * dx + dz * dz <= r * r) n++;
            }
            return n;
        }

        public double nearestDen(double x, double z) {
            double best = Double.MAX_VALUE;
            for (Den d : dens) best = Math.min(best, Math.sqrt((d.x() - x) * (d.x() - x) + (d.z() - z) * (d.z() - z)));
            return best;
        }

        public int bearRangesCovering(double x, double z) {
            int n = 0;
            for (Range r : bears) if (r.covers(x, z)) n++;
            return n;
        }

        public double nearestMoose(double x, double z) {
            double best = Double.MAX_VALUE;
            for (Range r : moose) best = Math.min(best, Math.sqrt((r.x() - x) * (r.x() - x) + (r.z() - z) * (r.z() - z)));
            return best;
        }

        public int hash() {
            StringBuilder b = new StringBuilder();
            for (Range r : moose) b.append(String.format(java.util.Locale.ROOT, "m%.0f,%.0f;", r.x(), r.z()));
            for (Range r : bears) b.append(String.format(java.util.Locale.ROOT, "%s%.0f,%.0f;", r.species(), r.x(), r.z()));
            for (Den d : dens) b.append(String.format(java.util.Locale.ROOT, "%s%.0f,%.0f;", d.kind(), d.x(), d.z()));
            b.append(trail.length);
            return com.yearofice.core.hash.StageHash.of(b.toString());
        }
    }

    public static Layers standIn(ValleyWorld vw, StartSiteParams.Life lp) {
        CoreGrid g = vw.g;
        List<Range> moose = pick(vw, lp, lp.moose(), "moose", 0x4D4F4F53);
        List<Range> bears = new ArrayList<>(pick(vw, lp, lp.blackBear(), "black_bear", 0x424C4B42));
        bears.addAll(pick(vw, lp, lp.grizzly(), "grizzly", 0x47525A5A));

        float[] slopeTan = g.slopes(vw.ground);
        List<Den> dens = new ArrayList<>();
        double lo = CoreGrid.tanDeg(lp.denSlopeMinDeg()), hi = CoreGrid.tanDeg(lp.denSlopeMaxDeg());
        for (Range r : bears) {
            // Eligible: a steep, high, dry slope inside the range; the hash picks among them, so dens spread over the walls.
            int best = -1, bestKey = Integer.MIN_VALUE;
            for (int c = 0; c < g.n * g.n; c += 3) {
                if (slopeTan[c] < lo || slopeTan[c] > hi || vw.wType[c] != WaterStage.DRY || vw.ground[c] < lp.denElevationMinM()) continue;
                double dx = g.worldX(c) - r.x(), dz = g.worldZ(c) - r.z();
                if (dx * dx + dz * dz >= r.radiusM() * r.radiusM()) continue;
                int key = Noise.hash(vw.seed.attemptSeed() ^ 0x44454E, c, dens.size());
                if (best < 0 || key > bestKey) { best = c; bestKey = key; }
            }
            if (best >= 0) dens.add(new Den(r.species(), g.worldX(best), g.worldZ(best)));
        }
        // One wolf den: a sandy bank within reach of water, picked by hash among the eligible cells.
        boolean[] wet = new boolean[g.n * g.n];
        for (int i = 0; i < wet.length; i++) wet[i] = vw.wType[i] != WaterStage.DRY;
        float[] dWater = g.distance(wet);
        double wlo = CoreGrid.tanDeg(lp.wolfSlopeMinDeg()), whi = CoreGrid.tanDeg(lp.wolfSlopeMaxDeg());
        int wolf = -1, wolfKey = Integer.MIN_VALUE;
        for (int c = 0; c < wet.length; c += 5) {
            if (wet[c] || dWater[c] > lp.wolfWaterMaxM() || slopeTan[c] < wlo || slopeTan[c] > whi) continue;
            if ((vw.sediment[c] & 0xFF) * 0.1 < lp.wolfSedimentMinM()) continue;
            int key = Noise.hash(vw.seed.attemptSeed() ^ 0x574F4C46, c, 7);
            if (wolf < 0 || key > wolfKey) { wolf = c; wolfKey = key; }
        }
        if (wolf >= 0) dens.add(new Den("wolf", g.worldX(wolf), g.worldZ(wolf)));

        // Trails: each range's centre to its nearest water access points (a lattice along the water), straight lines.
        int lat = Math.max(1, (int) Math.round(lp.trailWaterLatticeM() / g.cell));
        List<double[]> waterPts = new ArrayList<>();
        for (int k = lat / 2; k < g.n; k += lat) for (int i = lat / 2; i < g.n; i += lat) {
            int c = g.idx(i, k);
            if (wet[c]) waterPts.add(new double[] {g.worldX(c), g.worldZ(c)});
        }
        List<Double> trail = new ArrayList<>();
        List<Range> all = new ArrayList<>(moose);
        all.addAll(bears);
        for (Range r : all) {
            List<double[]> near = new ArrayList<>(waterPts);
            near.sort((a, b) -> Double.compare((a[0] - r.x()) * (a[0] - r.x()) + (a[1] - r.z()) * (a[1] - r.z()),
                    (b[0] - r.x()) * (b[0] - r.x()) + (b[1] - r.z()) * (b[1] - r.z())));
            for (int w = 0; w < Math.min(lp.trailWaterPoints(), near.size()); w++) {
                double[] q = near.get(w);
                double len = Math.sqrt((q[0] - r.x()) * (q[0] - r.x()) + (q[1] - r.z()) * (q[1] - r.z()));
                int steps = Math.max(1, (int) Math.ceil(len / lp.trailStepM()));
                for (int s2 = 0; s2 <= steps; s2++) {
                    trail.add(r.x() + (q[0] - r.x()) * s2 / steps);
                    trail.add(r.z() + (q[1] - r.z()) * s2 / steps);
                }
            }
        }
        double[] t = new double[trail.size()];
        for (int i = 0; i < t.length; i++) t[i] = trail.get(i);
        return new Layers(List.copyOf(moose), List.copyOf(bears), List.copyOf(dens), t);
    }

    private static List<Range> pick(ValleyWorld vw, StartSiteParams.Life lp, StartSiteParams.Species sp, String name, int salt) {
        CoreGrid g = vw.g;
        List<Integer> habitat = new ArrayList<>();
        for (String b : sp.habitat()) {
            for (int i = 0; i < vw.p.cover().classes().size(); i++) {
                if (vw.p.cover().classes().get(i).biome().equals("yearofice:" + b)) habitat.add(i);
            }
        }
        int step = Math.max(1, (int) Math.round(lp.habitatStepM() / g.cell));
        record Cand(double x, double z, double key) {}
        List<Cand> cands = new ArrayList<>();
        int[] off = {0, -step / 2, step / 2};
        for (int k = step / 2; k < g.n; k += step) for (int i = step / 2; i < g.n; i += step) {
            int hit = 0, tot = 0;
            for (int a : off) for (int b : off) {
                int ii = Math.max(0, Math.min(g.n - 1, i + a)), kk = Math.max(0, Math.min(g.n - 1, k + b));
                tot++;
                if (habitat.contains(vw.cover.classAt(g.idx(ii, kk)))) hit++;
            }
            double jitter = (Noise.hash(vw.seed.attemptSeed() ^ salt, i, k) >>> 8) / 16777216.0;
            cands.add(new Cand(g.worldX(g.idx(i, k)), g.worldZ(g.idx(i, k)), (double) hit / tot + 0.5 * jitter));
        }
        cands.sort((a, b) -> {
            int c = Double.compare(b.key(), a.key());
            return c != 0 ? c : Double.compare(a.x() * 4096 + a.z(), b.x() * 4096 + b.z());
        });
        List<Range> out = new ArrayList<>();
        for (Cand c : cands) {
            if (out.size() >= sp.count()) break;
            boolean apart = true;
            for (Range r : out) {
                double dx = r.x() - c.x(), dz = r.z() - c.z();
                if (dx * dx + dz * dz < sp.radiusM() * sp.radiusM()) { apart = false; break; }
            }
            if (apart) out.add(new Range(name, c.x(), c.z(), sp.radiusM()));
        }
        return out;
    }
}
