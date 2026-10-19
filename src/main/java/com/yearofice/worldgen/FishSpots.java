package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 4, step 5: fishing spots with quality and stock (Wildlife and fish › Fishing spot quality, Three minor
 * spot types; Fish stocks › Stock per spot; Waters and fish › New spot types).
 *
 * <p>Candidates come from hydrology: the pool below each fall, lake inlets and outlets, creek mouths, lake
 * drop-offs over 8 m, outsides of tight bends, weed shelves and spring leads (major), plus seams and deep river
 * holes (minor). Beaver ponds join from stage 5. Each spot's quality is
 * {@code q = clamp01(clamp(base + 0.1·[D > 2 m] + 0.05·[within 300 m below a lake or falls], band) · r)} with
 * r drawn in 0.8–1.2, and its starting stock is {@code round(q × capacity)} per species.
 *
 * <p>Boulder eddies wait for M2's boulders. The 40–120 count is over the major spots; when a map yields more,
 * the lowest-quality ones are dropped, so the count is a property of every map, not a hope.
 */
final class FishSpots {
    private FishSpots() {}

    /** Major spots kept free under the cap for stage 5's beaver ponds (two valleys of up to seven ponds). */
    static final int POND_RESERVE = 14;

    private record Cand(String type, int cell, int body, int bonusCell) {}

    static void run(Hydro h) {
        HydroParams.Spots sp = h.hp.spots();
        CoreGrid g = h.grid;
        List<Cand> cands = new ArrayList<>();
        int[] rcv = h.receiver;
        float[] ground = h.ground();

        for (WaterStage.Fall f : h.falls) cands.add(new Cand("pool_below_falls", f.foot(), f.bodyId(), f.foot()));
        for (int c : h.creekOrder) {
            int r = rcv[c];
            if (r >= 0 && (h.type[r] == Hydro.LAKE || h.type[r] == Hydro.OXBOW)) cands.add(new Cand("lake_inlet", r, h.body[r], c));
        }
        // Lakes: outlets, the steepest drop-off over 8 m, and weed-shelf spots spaced 60 m (one per 6 ha).
        int bodies = h.bodies.size() + 2;
        int[] drop = new int[bodies];
        double[] dropG = new double[bodies];
        Arrays.fill(drop, -1);
        for (int i = 0; i < h.total; i++) {
            if (h.bank[i] != Hydro.DROP_OFF || h.depth(i) < h.hp.water().dropOffMinDepthM()) continue;
            int b = h.body[i];
            double gg = g.slope(ground, i);
            if (b > 0 && b < bodies && gg > dropG[b]) { dropG[b] = gg; drop[b] = i; }
        }
        List<List<Integer>> shelf = new ArrayList<>();
        for (int b = 0; b < bodies; b++) shelf.add(new ArrayList<>());
        int[] want = new int[bodies];
        for (WaterStage.Lake l : h.lakes) want[l.bodyId()] = 1 + (int) (l.areaM2() / 60000);
        for (int i = 0; i < h.total; i++) {
            int idx = (int) (((long) i * 7919L + (h.seed.attemptSeed() & 0xFFFF)) % h.total);
            if (h.bank[idx] != Hydro.WEED_SHELF) continue;
            int b = h.body[idx];
            if (b <= 0 || b >= bodies || shelf.get(b).size() >= want[b]) continue;
            boolean far = true;
            for (int q : shelf.get(b)) {
                double dx = g.lx(q) - g.lx(idx), dz = g.lz(q) - g.lz(idx);
                if (dx * dx + dz * dz < 3600) { far = false; break; }
            }
            if (far) shelf.get(b).add(idx);
        }
        for (WaterStage.Lake l : h.lakes) {
            if (l.outlet() >= 0 && h.wet(l.outlet()) && h.type[l.outlet()] != Hydro.LAKE) {
                cands.add(new Cand("lake_outlet", l.outlet(), h.body[l.outlet()], l.outlet()));
            }
            if (l.maxDepthM() > h.hp.water().dropOffMinDepthM() && drop[l.bodyId()] >= 0) {
                cands.add(new Cand("lake_drop_off", drop[l.bodyId()], l.bodyId(), drop[l.bodyId()]));
            }
            for (int idx : shelf.get(l.bodyId())) cands.add(new Cand("weed_shelf", idx, l.bodyId(), idx));
        }
        for (WaterStage.Creek c : h.creeks) {
            if (!c.into().equals("main")) continue;
            int last = c.cells()[c.cells().length - 1];
            int r = rcv[last];
            if (r >= 0) cands.add(new Cand("creek_mouth", r, 1, last));
        }
        for (WaterStage.Spring s : h.springs) cands.add(new Cand("spring_lead", s.cell(), s.bodyId(), s.cell()));

        // Main river: outsides of tight bends, seams, deep holes.
        double wantR = h.hp.water().cutBankMaxRadiusWidths();
        for (Rivers.Line ln : h.main.lines()) {
            int lastApex = -1000;
            for (int i = 2; i + 2 < ln.size(); i++) {
                double k = Math.abs(ln.kappa()[i]);
                if (k * wantR * ln.w()[i] <= 1.0) continue;
                if (k < Math.abs(ln.kappa()[i - 1]) || k < Math.abs(ln.kappa()[i + 1])) continue;
                if ((i - lastApex) * Channels.DS < 3 * ln.w()[i]) continue;
                lastApex = i;
                double side = -Math.signum(ln.kappa()[i]) * ln.w()[i] / 3;
                int cell = g.cellAt(ln.x()[i] - ln.tz()[i] * side, ln.z()[i] + ln.tx()[i] * side);
                if (cell >= 0 && h.type[cell] == Hydro.MAIN) cands.add(new Cand("outside_bend", cell, 1, cell));
            }
        }
        Rivers.Line ax = h.main.lines().get(0);
        double s = 0, nextSeam = sp.seamSpacingM().draw(h.rng);
        for (int i = 1; i < ax.size(); i++) {
            s += Channels.DS;
            if (s < nextSeam) continue;
            s = 0;
            nextSeam = sp.seamSpacingM().draw(h.rng);
            if (ax.w()[i] < sp.seamMinWidthM()) continue;
            double side = (ax.kappa()[i] >= 0 ? 1 : -1) * ax.w()[i] / 4;
            int cell = g.cellAt(ax.x()[i] - ax.tz()[i] * side, ax.z()[i] + ax.tx()[i] * side);
            if (cell >= 0 && h.type[cell] == Hydro.MAIN) cands.add(new Cand("seam", cell, 1, cell));
        }
        List<Integer> holes = new ArrayList<>();
        for (int i = 0; i < h.total; i++) {
            if (h.type[i] != Hydro.MAIN || h.depth(i) <= sp.deepHoleMinDepthM()) continue;
            boolean far = true;
            for (int q : holes) {
                double dx = g.lx(q) - g.lx(i), dz = g.lz(q) - g.lz(i);
                if (dx * dx + dz * dz < 60 * 60) { far = false; break; }
            }
            if (far) holes.add(i);
        }
        for (int c : holes) cands.add(new Cand("deep_river_hole", c, 1, c));

        // The 300 m below lakes and falls.
        boolean[] below = belowMask(h, sp.belowBonusM());

        List<WaterStage.Spot> all = new ArrayList<>();
        for (Cand c : cands) {
            HydroParams.SpotType t = sp.type(c.type());
            double depth = Math.max(0, h.depth(c.cell()));
            double r = sp.variation().draw(h.rng);
            all.add(make(h, t, c.cell(), c.body(), depth, below[c.bonusCell()] || below[c.cell()], r));
        }
        addAll(h, all);
    }

    /** Builds one spot from its type, place and draw. Shared with stage 5's beaver ponds. */
    static WaterStage.Spot make(Hydro h, HydroParams.SpotType t, int cell, int body, double depth, boolean below, double r) {
        HydroParams.Spots sp = h.hp.spots();
        double pre = t.baseQ() + (depth > sp.deepBonusDepthM() ? sp.deepBonus() : 0) + (below ? sp.belowBonus() : 0);
        pre = Noise.clamp(pre, t.band().min(), t.band().max());
        double q = Noise.clamp01(pre * r);
        q = Math.round(q * 1000) / 1000.0;
        int[] k = new int[t.capacity().length];
        for (int i = 0; i < k.length; i++) k[i] = (int) Math.round(q * t.capacity()[i]);
        return new WaterStage.Spot(t.id(), cell, h.grid.worldX(cell), h.grid.worldZ(cell), t.radiusM(), q,
                Math.round(depth * 100) / 100.0, body, t.species(), k, t.major());
    }

    /** Dedupes by spacing (better spot wins) and caps the major count at the top of the range. */
    static void addAll(Hydro h, List<WaterStage.Spot> all) {
        HydroParams.Spots sp = h.hp.spots();
        CoreGrid g = h.grid;
        List<WaterStage.Spot> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> {
            int c = Double.compare(b.q(), a.q());
            if (c != 0) return c;
            c = a.type().compareTo(b.type());
            return c != 0 ? c : Integer.compare(a.cell(), b.cell());
        });
        List<WaterStage.Spot> kept = new ArrayList<>();
        int major = 0;
        for (WaterStage.Spot s : sorted) {
            boolean far = true;
            for (WaterStage.Spot k : kept) {
                double dx = g.lx(k.cell()) - g.lx(s.cell()), dz = g.lz(k.cell()) - g.lz(s.cell());
                if (dx * dx + dz * dz < sp.minSpacingM() * sp.minSpacingM()) { far = false; break; }
            }
            if (!far) continue;
            if (s.major()) {
                if (major >= (int) sp.count().max() - POND_RESERVE) continue;
                major++;
            }
            kept.add(s);
        }
        h.spots.addAll(kept);
    }

    /** Water cells within {@code reach} metres downstream of a lake outlet or a fall's foot. */
    static boolean[] belowMask(Hydro h, double reach) {
        CoreGrid g = h.grid;
        boolean[] out = new boolean[h.total];
        int[] rcv = h.receiver;
        List<Integer> starts = new ArrayList<>();
        for (WaterStage.Lake l : h.lakes) if (l.outlet() >= 0) starts.add(l.outlet());
        for (WaterStage.Fall f : h.falls) {
            if (f.onMain()) {
                double fx = g.lx(f.foot()), fz = g.lz(f.foot());
                int rc = (int) Math.ceil(reach / g.cell);
                for (int dz = -rc; dz <= rc; dz++) for (int dx = -rc; dx <= rc; dx++) {
                    int x = g.col(f.foot()) + dx, z = g.row(f.foot()) + dz;
                    if (!g.in(x, z)) continue;
                    int idx = g.idx(x, z);
                    if (h.type[idx] != Hydro.MAIN || h.ws[idx] > h.ws[f.foot()] + 0.05) continue;
                    double ex = g.lx(idx) - fx, ez = g.lz(idx) - fz;
                    if (ex * ex + ez * ez <= reach * reach) out[idx] = true;
                }
            } else {
                starts.add(f.foot());
            }
        }
        for (int st : starts) {
            double s = 0;
            int k = st;
            while (k >= 0 && s <= reach) {
                out[k] = true;
                int r = rcv[k];
                if (r < 0) break;
                if (h.type[r] == Hydro.MAIN) { out[r] = true; break; }
                s += Rivers.stepLen(g, k, r);
                k = r;
            }
        }
        return out;
    }

    static int majorCount(List<WaterStage.Spot> spots) {
        int c = 0;
        for (WaterStage.Spot s : spots) if (s.major()) c++;
        return c;
    }

    static final List<String> MAJOR_ORDER = Arrays.asList("pool_below_falls", "lake_inlet", "creek_mouth",
            "lake_drop_off", "outside_bend", "lake_outlet", "weed_shelf", "beaver_pond", "spring_lead");
}
