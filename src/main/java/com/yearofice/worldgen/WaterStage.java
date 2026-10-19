package com.yearofice.worldgen;

import com.yearofice.core.hash.StageHash;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stage 4, Water and rock ({@code stageName "water"}). Reads stages 1–3 and writes the water and rock layers
 * and lists, in the M1 build order: (1) the coarse 8 km network, (2) the main channel with its meanders,
 * oxbows and braids, then lakes and bogs, (3) creeks carved into the core, (4) falls and pools, (5) fishing
 * spots, (6) rock faces and shelter candidates. The main channel is laid before the lake flood because its
 * line comes from the coarse net; the flood then drains to it.
 *
 * <p>Height is never written: every bed lies in the carve layer, so stage 3's hash cannot move.
 */
public final class WaterStage {
    public static final String STAGE = "water";

    private WaterStage() {}

    // Layer values (shared with the debug map and the commands).
    public static final int DRY = Hydro.DRY, MAIN = Hydro.MAIN, CREEK = Hydro.CREEK, LAKE = Hydro.LAKE,
            OXBOW = Hydro.OXBOW, POOL = Hydro.POOL;
    public static final int NO_BANK = Hydro.NONE, BAR = Hydro.BAR, CUT_BANK = Hydro.CUT_BANK, BOG = Hydro.BOG, WEED_SHELF = Hydro.WEED_SHELF,
            DROP_OFF = Hydro.DROP_OFF;
    public static final int FACE_ROCK = Hydro.FACE_ROCK, IRON_OUTCROP = Hydro.IRON_OUTCROP, RUST_STAIN = Hydro.RUST_STAIN;
    public static final int F_THIN = Hydro.F_THIN, F_SPRING = Hydro.F_SPRING, F_LEAD = Hydro.F_LEAD,
            F_FALLS_POOL = Hydro.F_FALLS_POOL, F_FORD = Hydro.F_FORD, F_RIFFLE = Hydro.F_RIFFLE, F_SWEEPER = Hydro.F_SWEEPER;
    public static final int STILL = Hydro.STILL;

    /** A water body (the {@code yearofice_water} list): river reach, creek, lake or oxbow. */
    public record Body(int id, String kind, double areaM2, double meanDepthM, int outlet, double lengthM) {}

    public record Lake(int bodyId, String kind, int cells, double areaM2, double levelM, double maxDepthM,
                       double meanDepthM, int outlet, double cx, double cz, int outlets) {}

    /** A creek: its cells from source to mouth, length, catchment at the mouth, and what it flows into. */
    public record Creek(int bodyId, int[] cells, double lengthM, double catchmentM2, String into) {}

    public record Fall(int lip, int foot, double heightM, double runM, int bodyId, boolean onMain, boolean guaranteed,
                       double poolRadiusM, double faceHeightM) {
        Fall withGuaranteed() { return new Fall(lip, foot, heightM, runM, bodyId, onMain, true, poolRadiusM, faceHeightM); }

        Fall withPool(double r) { return new Fall(lip, foot, heightM, runM, bodyId, onMain, guaranteed, r, faceHeightM); }
    }

    public record Spring(int cell, int bodyId, double leadM) {}

    /** A fishing spot: type, cell, world position, radius, quality, depth, body, and starting stock per species. */
    public record Spot(String type, int cell, double x, double z, double radiusM, double q, double depthM, int bodyId,
                       List<String> species, int[] stock, boolean major) {}

    /** A rock face piece: its foot and top cells, height, aspect, size; shelter rank 1–20 when flagged. */
    public record Face(int foot, int top, double heightM, double aspectDeg, int cells, boolean shelter, int rank) {}

    public record IronCluster(int centre, int[] nodes, double elevationM) {}

    /** Everything stage 4 writes. Layers are row-major over the core's 2 m grid, real orientation. */
    public record Layers(int cells, byte[] type, float[] surface, float[] carve, short[] body, byte[] flowDir,
                         byte[] speed, float[] catchment, float[] width, byte[] bank, byte[] flags, byte[] flood,
                         byte[] rock, CoarseRivers.Net coarse, Rivers.Main main, List<Body> bodies, List<Lake> lakes,
                         List<Creek> creeks, List<Fall> falls, List<Spring> springs, List<Spot> spots,
                         List<Face> faces, List<IronCluster> iron, List<String> notes, int[] receiver) {

        /** Ground after the carve: what the chunk builder stands on. */
        public float[] ground(float[] eroded) {
            float[] g = new float[eroded.length];
            for (int i = 0; i < g.length; i++) g[i] = eroded[i] - carve[i];
            return g;
        }

        public double depth(float[] eroded, int i) { return type[i] == DRY ? 0 : surface[i] - (eroded[i] - carve[i]); }

        public double speedMS(int i) { return (speed[i] & 0xFF) * 0.05; }

        public int majorSpots() { return FishSpots.majorCount(spots); }

        public List<Face> shelters() {
            List<Face> out = new ArrayList<>();
            for (Face f : faces) if (f.shelter()) out.add(f);
            return out;
        }

        public int oxbowLakes() {
            int c = 0;
            for (Lake l : lakes) if (l.kind().equals("oxbow")) c++;
            return c;
        }

        public int hash() {
            StringBuilder b = new StringBuilder();
            b.append(StageHash.of(type)).append(':').append(StageHash.of(surface)).append(':').append(StageHash.of(carve))
                    .append(':').append(StageHash.of(shorts(body))).append(':').append(StageHash.of(flowDir)).append(':')
                    .append(StageHash.of(speed)).append(':').append(StageHash.of(catchment)).append(':')
                    .append(StageHash.of(bank)).append(':').append(StageHash.of(flags)).append(':')
                    .append(StageHash.of(flood)).append(':').append(StageHash.of(rock)).append(':').append(coarse.hash());
            for (Fall f : falls) b.append(String.format(Locale.ROOT, "|f%d,%d,%.3f,%.2f", f.lip(), f.foot(), f.heightM(), f.poolRadiusM()));
            for (Spot s : spots) b.append(String.format(Locale.ROOT, "|s%s,%d,%.3f", s.type(), s.cell(), s.q()));
            for (Face f : faces) if (f.rank() > 0) b.append(String.format(Locale.ROOT, "|r%d,%d", f.foot(), f.rank()));
            for (IronCluster c : iron) b.append("|i").append(c.centre());
            for (Spring s : springs) b.append("|p").append(s.cell());
            return StageHash.of(b.toString());
        }

        static byte[] shorts(short[] a) {
            byte[] out = new byte[a.length * 2];
            for (int i = 0; i < a.length; i++) { out[2 * i] = (byte) a[i]; out[2 * i + 1] = (byte) (a[i] >> 8); }
            return out;
        }

        /** Stage 4's checks (Pipeline › stage 4). Empty when every one passes. */
        public List<String> check(Archetype.Params arch, HydroParams hp) {
            List<String> fails = new ArrayList<>();
            int n = cells;
            // Water surface never rises downstream; every creek ends at the edge, a lake or the main river.
            for (Rivers.Line ln : main.lines()) {
                for (int i = 1; i < ln.size(); i++) {
                    if (ln.ws()[i] > ln.ws()[i - 1] + 1e-9) { fails.add("water: main river rises at point " + i); break; }
                }
                double ex = ln.x()[ln.size() - 1], ez = ln.z()[ln.size() - 1];
                double edge = Math.min(Math.min(ex, n * 2.0 - ex), Math.min(ez, n * 2.0 - ez));
                if (edge > 2.0) fails.add("water: main river ends " + edge + " m inside the core");
            }
            int rises = 0;
            for (Creek c : creeks) {
                for (int k = 0; k < c.cells().length; k++) {
                    int cell = c.cells()[k];
                    int r = receiver[cell];
                    if (r < 0) continue;
                    int t = type[r];
                    if (t != DRY && surface[r] > surface[cell] + 1e-3) rises++;
                }
                int last = c.cells()[c.cells().length - 1];
                int r = receiver[last];
                boolean ends = r < 0 || type[r] != DRY;
                if (!ends) fails.add("water: creek " + c.bodyId() + " ends on dry ground");
            }
            if (rises > 0) fails.add("water: creek surface rises downstream at " + rises + " cells");
            for (Lake l : lakes) if (l.outlets() != 1) fails.add("water: lake " + l.bodyId() + " has " + l.outlets() + " outlets");
            if (falls.size() < arch.minimumFalls()) fails.add("water: " + falls.size() + " falls, need " + arch.minimumFalls());
            int shelters = shelters().size();
            if (shelters < hp.rock().minShelters()) fails.add("water: " + shelters + " shelter faces, need " + hp.rock().minShelters());
            return fails;
        }
    }

    public static Layers run(SeedStage.SeedParams seed, Landform.Layers land, Erosion.Layers ero, WorldgenParams p,
                             Archetype.Params arch) {
        Hydro h = new Hydro(seed, land, ero, p, arch);
        h.coarse = CoarseRivers.run(seed, land, p);
        Rivers.mainChannel(h);
        Lakes.run(h);
        Rivers.creeks(h);
        Falls.run(h);
        Rivers.banks(h);
        FishSpots.run(h);
        RockFaces.run(h);
        float[] carve = new float[h.total];
        for (int i = 0; i < h.total; i++) carve[i] = h.carveMain[i] + h.carveCreek[i] + h.carvePool[i];
        for (int i = 0; i < h.total; i++) {
            if (h.type[i] == Hydro.LAKE || h.type[i] == Hydro.OXBOW) { h.dir[i] = (byte) Hydro.STILL; h.speed[i] = 0; }
        }
        return new Layers(h.n, h.type, h.ws, carve, h.body, h.dir, h.speed, h.catchment, h.width, h.bank, h.flags,
                h.flood, h.rock, h.coarse, h.main, List.copyOf(h.bodies), List.copyOf(h.lakes), List.copyOf(h.creeks),
                List.copyOf(h.falls), List.copyOf(h.springs), List.copyOf(h.spots), List.copyOf(h.faces),
                List.copyOf(h.iron), List.copyOf(h.notes), h.receiver);
    }
}
