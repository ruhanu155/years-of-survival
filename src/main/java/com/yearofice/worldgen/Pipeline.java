package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the creation stages built so far (1 Seed, 2 Landform, 3 Erosion, 4 Water and rock, 5 Places) for the
 * home valley, checks each stage, and rerolls with attempt k + 1 on a failed check, up to {@code max_attempts}
 * (20). Stages 4–5 run only once stages 1–3 pass, so a failed landform never pays for them.
 *
 * <p>Pure Java with no Minecraft imports, no clock and no shared state: the same level seed and generator
 * version give the same layers on every JVM. Callers time it; the pipeline never reads the clock.
 */
public final class Pipeline {
    private final WorldgenParams params;
    private final Archetype.Params[] archetypes;
    private final List<SetPieceDef> setPieces;

    public Pipeline(WorldgenParams params, Archetype.Params[] archetypes) {
        this(params, archetypes, SetPieceDef.loadAll());
    }

    public Pipeline(WorldgenParams params, Archetype.Params[] archetypes, List<SetPieceDef> setPieces) {
        this.params = params;
        this.archetypes = archetypes;
        this.setPieces = setPieces;
    }

    /** A pipeline on the data shipped in the jar. */
    public static Pipeline standard() { return new Pipeline(WorldgenParams.load(), Archetype.loadAll()); }

    public List<SetPieceDef> setPieces() { return setPieces; }

    public WorldgenParams params() { return params; }

    public Archetype.Params archetype(Archetype a) { return archetypes[a.ordinal()]; }

    /** Summary numbers of one attempt, as the checks and tests read them. */
    public record Stats(double minM, double maxM, double floorMedianM, double floorMinM, double floorMaxM,
                        int nanCount, double massBalance, int hardFaces, double hardFacesKept) {}

    /** One attempt's layers and verdict. {@code failures} is empty when every check passed. */
    public record Result(SeedStage.SeedParams seed, Landform.Layers landform, Erosion.Layers erosion, Stats stats,
                         WaterStage.Layers water, PlacesStage.Layers places, List<String> failures,
                         List<String> earlierFailures) {
        public boolean passed() { return failures.isEmpty(); }

        public int attempt() { return seed.attempt(); }

        /** The height hash: eroded height, the layer later stages read. */
        public int heightHash() { return erosion.heightHash(); }

        public int landformHash() { return landform.hash(); }

        /** Stage 4's fingerprint (0 when stages 1–3 failed and it never ran). */
        public int waterHash() { return water == null ? 0 : water.hash(); }

        public int placesHash() { return places == null ? 0 : places.hash(); }

        /** The full M1 layer hash: stages 2–5 together. */
        public int m1Hash() {
            return StageHashes.combine(landformHash(), heightHash(), erosion.sedimentHash(), waterHash(), placesHash());
        }

        /** Ground after stage 4's carve. */
        public float[] ground() { return water.ground(erosion.height()); }

        /** Every fishing spot: stage 4's and stage 5's beaver ponds. */
        public List<WaterStage.Spot> spots() {
            List<WaterStage.Spot> all = new ArrayList<>(water.spots());
            all.addAll(places.pondSpots());
            return all;
        }
    }

    /** Stage 1 alone, for the archetype-share test and the debug command. Under 1 ms. */
    public SeedStage.SeedParams seed(long levelSeed, int attempt) { return SeedStage.run(levelSeed, attempt, params); }

    /** Stages 1–2 alone. */
    public Landform.Layers landform(SeedStage.SeedParams seed) { return Landform.run(seed, params, archetypes); }

    /** One attempt of stages 1–3 with every check. */
    public Result attempt(long levelSeed, int attempt) {
        List<String> fails = new ArrayList<>();
        SeedStage.SeedParams seed = seed(levelSeed, attempt);
        fails.addAll(seed.check(params));
        Landform.Layers land = landform(seed);
        Erosion.Layers ero = Erosion.run(seed, land, params, archetypes[seed.archetype().ordinal()]);
        Stats st = stats(land, ero);
        WorldgenParams.Elevation el = params.elevation();
        if (countNaN(land.height()) + countNaN(land.coarse()) > 0) fails.add("landform: NaN");
        if (st.nanCount() > 0) fails.add("erosion: NaN");
        if (st.minM() < el.minM() || st.maxM() > el.maxM()) {
            fails.add("height: " + st.minM() + "–" + st.maxM() + " m outside " + el.minM() + "–" + el.maxM());
        }
        if (Math.abs(st.floorMedianM() - el.floorM()) > el.floorToleranceM()) {
            fails.add("floor: median " + st.floorMedianM() + " m");
        }
        if (st.massBalance() > params.checks().massBalanceFraction()) fails.add("erosion: mass balance " + st.massBalance());
        if (st.hardFaces() > 0 && st.hardFacesKept() < params.checks().hardFaceKeepFraction()) {
            fails.add("erosion: hard faces kept " + st.hardFacesKept());
        }
        WaterStage.Layers water = null;
        PlacesStage.Layers places = null;
        if (fails.isEmpty()) {
            Archetype.Params arch = archetypes[seed.archetype().ordinal()];
            water = WaterStage.run(seed, land, ero, params, arch);
            fails.addAll(water.check(arch, params.hydro()));
            places = PlacesStage.run(seed, land, ero, water, params, setPieces);
            fails.addAll(places.check(setPieces, water, water.ground(ero.height()), new CoreGrid(params, seed.mirror()),
                    params.hydro()));
        }
        return new Result(seed, land, ero, st, water, places, List.copyOf(fails), List.of());
    }

    /** Attempts 0, 1, ... until one passes or {@code max_attempts} run out (then the last is returned). */
    public Result run(long levelSeed) { return run(levelSeed, 0); }

    public Result run(long levelSeed, int firstAttempt) {
        List<String> earlier = new ArrayList<>();
        Result r = null;
        int max = params.seed().maxAttempts();
        for (int k = firstAttempt; k < firstAttempt + max; k++) {
            r = attempt(levelSeed, k);
            if (r.passed()) break;
            for (String f : r.failures()) earlier.add("attempt " + k + ": " + f);
        }
        return new Result(r.seed(), r.landform(), r.erosion(), r.stats(), r.water(), r.places(), r.failures(),
                List.copyOf(earlier));
    }

    /** Eroded height under each in-core river path point: the valley floor profile. */
    public double[] floorProfile(Landform.Layers land, float[] height) {
        RiverPath path = land.path();
        int n = land.cells();
        double cell = params.grid().cellM(), L = params.grid().coreSizeM();
        double[] out = new double[path.corePointCount()];
        for (int i = 0; i < out.length; i++) {
            double x = path.coreX(i), z = path.coreZ(i);
            if (land.mirror()) x = L - x;
            int ci = (int) Math.max(0, Math.min(n - 1, Math.floor(x / cell)));
            int ck = (int) Math.max(0, Math.min(n - 1, Math.floor(z / cell)));
            out[i] = height[ck * n + ci];
        }
        return out;
    }

    Stats stats(Landform.Layers land, Erosion.Layers ero) {
        float[] h = ero.height();
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        double sumBefore = 0, sumAfter = 0;
        for (int i = 0; i < h.length; i++) {
            double v = h[i];
            if (v < min) min = v;
            if (v > max) max = v;
            sumAfter += v;
            sumBefore += land.height()[i];
        }
        double[] floor = floorProfile(land, h);
        double[] sorted = floor.clone();
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];

        double cellArea = params.grid().cellM() * params.grid().cellM();
        double erodedCells = ero.erodedM3() / cellArea;
        double balance = erodedCells > 0 ? Math.abs(sumAfter - sumBefore) / erodedCells : 0;

        // Hard faces: hard cells steeper than the face slope in stage 2. How many stay that steep?
        int n = land.cells();
        double rise = StrictMath.tan(Math.toRadians(params.checks().hardFaceSlopeDeg())) * params.grid().cellM();
        float[] before = land.height();
        int faces = 0, kept = 0;
        for (int z = 1; z < n - 1; z++) {
            for (int x = 1; x < n - 1; x++) {
                int idx = z * n + x;
                if (land.hardnessAt(idx) <= params.erosion().hardThreshold()) continue;
                if (maxDrop(before, n, idx) > rise) {
                    faces++;
                    if (maxDrop(h, n, idx) > rise) kept++;
                }
            }
        }
        return new Stats(min, max, median, sorted[0], sorted[sorted.length - 1], countNaN(h), balance, faces,
                faces > 0 ? (double) kept / faces : 1.0);
    }

    private static double maxDrop(float[] h, int n, int idx) {
        double c = h[idx];
        double a = Math.abs(c - h[idx - 1]), b = Math.abs(c - h[idx + 1]);
        double d = Math.abs(c - h[idx - n]), e = Math.abs(c - h[idx + n]);
        return Math.max(Math.max(a, b), Math.max(d, e));
    }

    /** Combines stage hashes into one fingerprint. */
    static final class StageHashes {
        private StageHashes() {}

        static int combine(int... hs) {
            StringBuilder b = new StringBuilder();
            for (int h : hs) b.append(Integer.toUnsignedString(h)).append(':');
            return com.yearofice.core.hash.StageHash.of(b.toString());
        }
    }

    static int countNaN(float[] a) {
        int c = 0;
        for (float v : a) if (Float.isNaN(v) || Float.isInfinite(v)) c++;
        return c;
    }
}
