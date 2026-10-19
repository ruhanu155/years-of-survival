package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stage 5, Places ({@code stageName "places"}). Reads stages 1–4 and writes the set-piece placements, their
 * footprint mask, the landmarks and the beaver-pond fishing spots. Trapper caches (they need game trails, M3)
 * and mineral licks (they need clay and cover, M2) join later.
 */
public final class PlacesStage {
    public static final String STAGE = "places";

    private PlacesStage() {}

    public record Layers(byte[] footprint, List<SetPieces.Placement> placements, List<Landmarks.Landmark> landmarks,
                         List<WaterStage.Spot> pondSpots, List<String> notes) {

        public List<SetPieces.Placement> of(String id) {
            List<SetPieces.Placement> out = new ArrayList<>();
            for (SetPieces.Placement q : placements) if (q.id().equals(id)) out.add(q);
            return out;
        }

        /** The provisional start: the outpost, or null. */
        public SetPieces.Placement outpost() {
            List<SetPieces.Placement> o = of("yearofice:outpost");
            return o.isEmpty() ? null : o.get(0);
        }

        /** Distance of a placement from the provisional start, in metres (the "typical distance" report). */
        public double fromStart(SetPieces.Placement q) {
            SetPieces.Placement o = outpost();
            if (o == null) return Double.NaN;
            double dx = q.x() - o.x(), dz = q.z() - o.z();
            return Math.sqrt(dx * dx + dz * dz);
        }

        public int hash() {
            StringBuilder b = new StringBuilder();
            b.append(StageHash.of(footprint));
            for (SetPieces.Placement q : placements) {
                b.append(String.format(Locale.ROOT, "|%s#%d,%s,%s,%.2f,%.2f,%.1f,%d,%d,%d,%b", q.id(), q.instance(),
                        q.variant(), q.site(), q.x(), q.z(), q.y(), q.rotation(), q.footprintX(), q.footprintZ(), q.coarse()));
            }
            for (Landmarks.Landmark l : landmarks) {
                b.append(String.format(Locale.ROOT, "|L%d,%s,%.1f,%.1f,%.1f,%d", l.region(), l.kind(), l.x(), l.z(), l.heightM(),
                        l.visiblePoints()));
            }
            for (WaterStage.Spot s : pondSpots) b.append(String.format(Locale.ROOT, "|P%d,%.3f", s.cell(), s.q()));
            return StageHash.of(b.toString());
        }

        /**
         * Stage 5's checks: every required piece placed, none in water or on a cell over 25°, no footprints
         * overlapping, and a landmark seen from 2 km in every region.
         */
        public List<String> check(List<SetPieceDef> defs, WaterStage.Layers w, float[] ground, CoreGrid g, HydroParams hp) {
            List<String> fails = new ArrayList<>(notes.stream().filter(s -> s.endsWith("no site")).toList());
            double cap = CoreGrid.tanDeg(hp.places().maxSlopeDeg());
            for (SetPieceDef d : defs) {
                int c = of(d.id()).size();
                if (c < d.count().min() || c > d.count().max()) fails.add("places: " + d.id() + " count " + c);
            }
            int[] owner = new int[footprint.length];
            for (int i = 0; i < footprint.length; i++) {
                if (footprint[i] == 0) continue;
                if (w.type()[i] != WaterStage.DRY) { fails.add("places: footprint " + footprint[i] + " in water"); break; }
                if (g.slope(ground, i) > cap) { fails.add("places: footprint " + footprint[i] + " over 25 degrees"); break; }
            }
            for (int k = 0; k < placements.size(); k++) {
                SetPieces.Placement q = placements.get(k);
                if (q.coarse()) continue;
                int cells = 0;
                for (byte b : footprint) if ((b & 0xFF) == k + 1) cells++;
                int want = (int) (Math.ceil(q.footprintX() / g.cell) * Math.ceil(q.footprintZ() / g.cell));
                if (cells < want * 0.75) fails.add("places: footprint of " + q.id() + " overlapped or clipped");
            }
            boolean[] seen = new boolean[16];
            for (Landmarks.Landmark l : landmarks) if (l.passes(hp.landmarks().minPoints())) seen[l.region()] = true;
            for (int r = 0; r < 16; r++) if (!seen[r]) fails.add("places: no landmark seen from 2 km in " + SeedStage.regionCode(r));
            return fails;
        }
    }

    public static Layers run(SeedStage.SeedParams seed, Landform.Layers land, Erosion.Layers ero, WaterStage.Layers w,
                             WorldgenParams p, List<SetPieceDef> defs) {
        SetPieces.Sites sites = new SetPieces.Sites(seed, land, ero, w, p);
        byte[] mask = new byte[sites.g.n * sites.g.n];
        List<String> notes = new ArrayList<>();
        List<SetPieces.Placement> placements = SetPieces.run(sites, defs, p, mask, notes);
        List<Landmarks.Landmark> landmarks = Landmarks.run(seed, sites.field, p, defs, placements, w, sites.g, sites.ground);

        // Beaver ponds: fishing spots along each core valley's reach.
        Mulberry32 rng = SeedStage.rng(seed.attemptSeed(), STAGE + ":ponds");
        HydroParams.Spots sp = p.hydro().spots();
        HydroParams.SpotType t = sp.type("beaver_pond");
        List<WaterStage.Spot> ponds = new ArrayList<>();
        for (SetPieces.Placement q : placements) {
            if (q.ponds() <= 0 || q.coarse() || q.reachStart() < 0) continue;
            int[] cells = null;
            int a = -1, b = -1;
            for (WaterStage.Creek c : w.creeks()) {
                for (int k = 0; k < c.cells().length; k++) {
                    if (c.cells()[k] == q.reachStart()) a = k;
                    if (c.cells()[k] == q.reachEnd()) b = k;
                }
                if (a >= 0 && b >= 0) { cells = c.cells(); break; }
                a = -1;
                b = -1;
            }
            if (cells == null) continue;
            for (int j = 0; j < q.ponds(); j++) {
                int cell = cells[a + (int) ((b - a) * (j + 0.5) / q.ponds())];
                double r = sp.variation().draw(rng);
                double pre = Noise.clamp(t.baseQ(), t.band().min(), t.band().max());
                double qv = Math.round(Noise.clamp01(pre * r) * 1000) / 1000.0;
                int[] stock = new int[t.capacity().length];
                for (int i = 0; i < stock.length; i++) stock[i] = (int) Math.round(qv * t.capacity()[i]);
                ponds.add(new WaterStage.Spot(t.id(), cell, sites.g.worldX(cell), sites.g.worldZ(cell), t.radiusM(), qv, 1.5,
                        w.body()[cell], t.species(), stock, true));
            }
        }
        return new Layers(mask, List.copyOf(placements), List.copyOf(landmarks), List.copyOf(ponds), List.copyOf(notes));
    }
}
