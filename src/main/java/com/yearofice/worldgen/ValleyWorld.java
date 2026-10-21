package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * The layers of one world, bound for chunk building (Minecraft development › The generator; Terrain to blocks).
 * It keeps only what the column builder, the biome source, the tree plan and the set-piece structures read
 * (about 20 MB for the core) and drops the rest of the pipeline result.
 *
 * <p>Inside the core (region B3) every column reads the 2 m layers: ground after stage 4's carve, water type
 * and surface, bank, rock, sediment, the set-piece footprints and stage 6's cover. Beyond it, the 32 m coarse
 * field plus a small detail noise that fades in over the first 64 m, with cover from the same rules and no
 * water: the other regions' 2 m layers come with their region builds.
 *
 * <p>Pure: no Minecraft imports. Every value is a function of the level seed, so two chunk workers asking for
 * the same column get the same answer in any order.
 */
public final class ValleyWorld {
    public final WorldgenParams p;
    public final SeedStage.SeedParams seed;
    public final CoreGrid g;
    public final int n;
    final float[] ground;
    final byte[] wType, bank, rock, wFlags, sediment;
    final float[] ws;
    final float[] coarse;
    final int cn;
    public final Cover.Layers cover;
    public final List<SetPieces.Placement> placements;
    public final List<SetPieceDef> defs;
    public final List<WaterStage.Spot> spots;
    public final List<CoalSeams.Seam> seams;
    final List<CoalSeams.Seam> bluffs;
    final byte[] node;          // per core cell: 0, IRON, COAL
    final short[] coalBand;     // seam band y per coal cell
    final boolean[] fallsZone;  // cells where a stepped water surface may flow (falls and their pools)
    public final TerrainPalette palette = new TerrainPalette();
    final int[] classSoil, classSurface, classBed;
    final int clayBank, riverSand, rockClass;
    final int detailSeed, treeSeed, plantSeed;

    /** The seed of the vegetation pass ({@link PlantPlan}). */
    public int plantSeed() { return plantSeed; }
    public final int m1Hash, coverHash;
    public final SliceCut.Window slice;
    /** The slice valley's fishing spot: the best one in the cut nearest the start, dug to a 2-block pool. */
    public final WaterStage.Spot sliceSpot;
    final List<String> notes;

    static final byte NODE_IRON = 1, NODE_COAL = 2;

    /** Stage 5's placements, outpost included: the pre-start world the M0–M2 tests build. */
    ValleyWorld(WorldgenParams p, Pipeline.Result r, Cover.Layers cover, List<SetPieceDef> defs) {
        this(p, r, cover, defs, r.places().placements(), false);
    }

    /**
     * {@code placements} replace stage 5's list: the start-site search passes the list without an outpost, then with the
     * winner ({@code startChosen}, which also keeps surface iron off the ring round the site).
     */
    ValleyWorld(WorldgenParams p, Pipeline.Result r, Cover.Layers cover, List<SetPieceDef> defs, List<SetPieces.Placement> placements,
                boolean startChosen) {
        this.p = p;
        this.seed = r.seed();
        this.g = new CoreGrid(p, r.seed().mirror());
        this.n = g.n;
        WaterStage.Layers w = r.water();
        this.ground = w.ground(r.erosion().height());
        this.wType = w.type();
        this.ws = w.surface();
        this.bank = w.bank();
        this.rock = w.rock();
        this.wFlags = w.flags();
        this.sediment = r.erosion().sediment();
        this.coarse = r.landform().coarse();
        this.cn = r.landform().coarseCells();
        this.cover = cover;
        this.placements = placements;
        this.defs = defs;
        this.spots = r.spots();
        this.m1Hash = r.m1Hash();
        this.coverHash = cover.hash();
        this.notes = new ArrayList<>(r.water().notes());
        this.detailSeed = SeedStage.stageSeed(r.seed().attemptSeed(), "terrain:detail");
        this.treeSeed = SeedStage.stageSeed(r.seed().attemptSeed(), "cover:trees");
        this.plantSeed = SeedStage.stageSeed(r.seed().attemptSeed(), "cover:plants");

        Cover.Params cp = p.cover();
        int k = cp.classes().size();
        classSoil = new int[k];
        classSurface = new int[k];
        classBed = new int[k];
        for (int i = 0; i < k; i++) {
            Cover.ClassDef d = cp.classes().get(i);
            classSoil[i] = palette.index(d.soil());
            classSurface[i] = palette.index(d.surface());
            classBed[i] = palette.index(d.bed());
        }
        clayBank = palette.index("clay_bank");
        rockClass = Math.max(0, cp.index("rock_face"));
        riverSand = palette.index("river_sand");

        node = new byte[n * n];
        coalBand = new short[n * n];
        SetPieces.Placement start = outpost();
        // Surface iron stays at least iron.min_m from the start site (Resource placement): stage 4 laid the clusters
        // before the site existed, so the ones inside that ring are left off the node layer once the site is chosen.
        StartSiteParams ssp = start == null || !startChosen ? null : StartSiteParams.load();
        double ironMin = ssp != null && ssp.ironPrune() ? ssp.ironMinM() : 0;
        for (WaterStage.IronCluster c : w.iron()) {
            for (int cell : c.nodes()) {
                if (start != null) {
                    double dx = g.worldX(cell) - start.x(), dz = g.worldZ(cell) - start.z();
                    if (Math.sqrt(dx * dx + dz * dz) < ironMin) continue;
                }
                node[cell] = NODE_IRON;
            }
        }
        this.seams = CoalSeams.run(this, start == null ? g.originX + g.size / 2 : start.x(),
                start == null ? g.originZ + g.size / 2 : start.z(), w.faces());
        List<CoalSeams.Seam> bl = new ArrayList<>();
        for (CoalSeams.Seam s : seams) {
            for (int cell : s.cells()) { node[cell] = NODE_COAL; coalBand[cell] = (short) s.bandY(); }
            if (s.coarse()) bl.add(s);
        }
        this.bluffs = List.copyOf(bl);

        fallsZone = new boolean[n * n];
        int rad = (int) Math.ceil(p.terrain().fallsFlowRadiusM() / g.cell);
        for (WaterStage.Fall f : w.falls()) {
            int ax = g.col(f.lip()), az = g.row(f.lip()), bx = g.col(f.foot()), bz = g.row(f.foot());
            int steps = Math.max(Math.abs(bx - ax), Math.abs(bz - az)) + 1;
            for (int s = 0; s <= steps; s++) {
                int cx = ax + (bx - ax) * s / steps, cz = az + (bz - az) * s / steps;
                for (int dz = -rad; dz <= rad; dz++) for (int dx = -rad; dx <= rad; dx++) {
                    if (g.in(cx + dx, cz + dz)) fallsZone[g.idx(cx + dx, cz + dz)] = true;
                }
            }
        }
        this.slice = SliceCut.choose(this);
        this.sliceSpot = pickSliceSpot();
    }

    /** Food's guarantee (RECONCILE 236): a usable spot within this many blocks of the start bank. */
    public static final double SPOT_NEAR_START_M = 60;

    /** The made-up spot when no real one lies within {@link #SPOT_NEAR_START_M}; null otherwise. Not in {@link #spots}, so no stage hash moves. */
    private WaterStage.Spot nearSpot;

    private WaterStage.Spot pickSliceSpot() {
        SetPieces.Placement start = outpost();
        double sx = start == null ? slice.centreX() : start.x(), sz = start == null ? slice.centreZ() : start.z();
        WaterStage.Spot best = null, nearest = null;
        double key = -Double.MAX_VALUE, nd = Double.MAX_VALUE;
        for (WaterStage.Spot s : spots) {
            if (!slice.contains(s.x(), s.z())) continue;
            double dx = s.x() - sx, dz = s.z() - sz, d = Math.sqrt(dx * dx + dz * dz);
            if (d < nd) { nd = d; nearest = s; }
            if (d > SPOT_NEAR_START_M) continue;
            double k = s.q() * 1000 - d;
            if (k > key) { key = k; best = s; }
        }
        if (best != null) return best;
        // None close enough: make a pool spot at the nearest water (the start site guarantees water within 40 m), typed and stocked like the nearest real one.
        double bd = Double.MAX_VALUE;
        int bx = 0, bz = 0;
        for (int dz = -60; dz <= 60; dz += 2) for (int dx = -60; dx <= 60; dx += 2) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > SPOT_NEAR_START_M || d >= bd) continue;
            int x = (int) Math.floor(sx) + dx, z = (int) Math.floor(sz) + dz;
            if (!slice.contains(x + 0.5, z + 0.5) || !allWet(x, z, 3)) continue;
            bd = d; bx = x; bz = z;
        }
        if (nearest == null || bd == Double.MAX_VALUE) return nearest;
        int cell = cellOf(bx, bz);
        int[] stock = new int[nearest.stock().length];
        for (int i = 0; i < stock.length; i++) stock[i] = (int) Math.round(nearest.stock()[i] * 0.55 / Math.max(0.1, nearest.q()));
        nearSpot = new WaterStage.Spot(nearest.type(), cell, bx + 0.5, bz + 0.5, nearest.radiusM(), 0.55, 1.5, nearest.bodyId(),
                nearest.species(), stock, false);
        return nearSpot;
    }

    private boolean allWet(int x, int z, int r) {
        int c = cellOf(x, z);
        return c >= 0 && wType[c] != WaterStage.DRY;
    }

    /** Pool depth in blocks the slice spot is dug to, over this radius around it (Food's stand-in rule: 2 × 2, 1.5 m). */
    static final int SPOT_POOL_BLOCKS = 2;
    static final double SPOT_POOL_RADIUS = 2.5;

    private volatile StartSite.Outcome startOutcome;

    /**
     * Runs stages 1–6 and the start-site search for a level seed and binds the result: attempt k + 1 on any failed
     * check in stages 1–7, up to {@code seed.max_attempts}. About 5 s; call it off the main thread where it can be.
     */
    public static ValleyWorld build(Pipeline pipe, long levelSeed) { return build(pipe, levelSeed, null); }

    /** As {@link #build(Pipeline, long)}, reading and writing the winning attempt in {@code cacheDir} (null: no cache). */
    public static ValleyWorld build(Pipeline pipe, long levelSeed, java.nio.file.Path cacheDir) {
        int first = LayerCache.load(cacheDir, levelSeed);
        StartSite.Outcome o;
        try {
            o = StartSite.search(pipe, levelSeed, StartSiteParams.load(), Math.max(0, first));
        } catch (IllegalStateException e) {
            if (first <= 0) throw e;
            o = StartSite.search(pipe, levelSeed, StartSiteParams.load());
            first = -1;
        }
        if (first != o.result().attempt()) LayerCache.store(cacheDir, levelSeed, o.result().attempt());
        ValleyWorld w = new ValleyWorld(pipe.params(), o.result(), o.cover(), pipe.setPieces(), o.placements(), true);
        w.startOutcome = o;
        return w;
    }

    /** The start-site search that made this world: every attempt's per-check report ({@code /yoi seed}). Null for test builds. */
    public StartSite.Outcome startOutcome() { return startOutcome; }

    /** The winning site, or null if this world was not built through the search. */
    public StartSite.Site start() { return startOutcome == null ? null : startOutcome.site(); }

    /** The gravel-bar block the Endless SOS flight returns to (x, y, z), or null. */
    public int[] sosReturn() {
        StartSite.Site s = start();
        return s == null || s.sosReturn() == null ? null : s.sosReturn().clone();
    }

    // ------------------------------------------------------------------ places

    public boolean inCore(double wx, double wz) {
        double lx = wx - g.originX, lz = wz - g.originZ;
        return lx >= 0 && lz >= 0 && lx < g.size && lz < g.size;
    }

    /** Core cell of a block column, or −1 outside the core. */
    public int cellOf(int x, int z) { return g.cellAt(x + 0.5 - g.originX, z + 0.5 - g.originZ); }

    public SetPieces.Placement outpost() {
        for (SetPieces.Placement q : placements) if (q.id().equals("yearofice:outpost")) return q;
        return null;
    }

    /** A placement's block rectangle: min x, min z, size x, size z. */
    public static int[] rect(SetPieces.Placement q) {
        int x0 = (int) Math.floor(q.x() - q.footprintX() / 2.0), z0 = (int) Math.floor(q.z() - q.footprintZ() / 2.0);
        return new int[] {x0, z0, q.footprintX(), q.footprintZ()};
    }

    /** The pad's surface block y: templates stand on the block above it. */
    public static int padY(SetPieces.Placement q) { return (int) Math.round(q.y()); }

    public SetPieceDef def(String id) {
        for (SetPieceDef d : defs) if (d.id().equals(id)) return d;
        return null;
    }

    public SetPieceDef.Variant variant(SetPieces.Placement q) {
        SetPieceDef d = def(q.id());
        if (d == null) return null;
        for (SetPieceDef.Variant v : d.variants()) if (v.name().equals(q.variant())) return v;
        return null;
    }

    /**
     * The start: the block in front of the outpost door (the variant's {@code spawn_offset}, turned with the
     * template), standing on the pad; and the yaw facing out of the door. Falls back to the pad centre.
     */
    public int[] spawn() {
        SetPieces.Placement q = outpost();
        if (q == null) {
            int x = (int) (g.originX + g.size / 2), z = (int) (g.originZ + g.size / 2);
            Columns c = columns(x, z, 1, 1);
            return new int[] {x, c.top(0) + 1, z, 0};
        }
        int[] r = rect(q);
        SetPieceDef.Variant v = variant(q);
        int[] off = v == null || v.spawnOffset() == null ? new int[] {v == null ? r[2] / 2 : v.footprintX() / 2, 0, r[3] / 2}
                : v.spawnOffset();
        int sx = v == null ? r[2] : v.footprintX(), sz = v == null ? r[3] : v.footprintZ();
        int[] t = turn(off[0], off[2], sx, sz, q.rotation());
        // Door faces south in the template (+z); the rotation turns that direction too. Yaw: south 0, west 90, north 180, east 270.
        int yaw = new int[] {0, 90, 180, 270}[q.rotation()];
        return new int[] {r[0] + t[0], padY(q) + 1 + off[1], r[1] + t[1], yaw};
    }

    /** A template-local (x, z) after a rotation, relative to the rotated box's minimum corner (Minecraft's turns). */
    public static int[] turn(int x, int z, int sx, int sz, int rotation) {
        return switch (rotation) {
            case 1 -> new int[] {sz - 1 - z, x};          // clockwise 90
            case 2 -> new int[] {sx - 1 - x, sz - 1 - z}; // 180
            case 3 -> new int[] {z, sx - 1 - x};          // counterclockwise 90
            default -> new int[] {x, z};
        };
    }

    /** Region code (A1–D4) of a block column. */
    public String regionCode(int x, int z) {
        double half = p.grid().worldSizeM() / 2.0, rs = p.grid().worldSizeM() / 4.0;
        int col = (int) Math.floor((x + half) / rs), row = (int) Math.floor((z + half) / rs);
        if (col < 0 || row < 0 || col > 3 || row > 3) return "beyond";
        return SeedStage.regionCode(row * 4 + col);
    }

    /** The raw layer values at a column, for {@code /yoi show current}. */
    public String layerLine(int x, int z) {
        int cell = cellOf(x, z);
        if (cell < 0) return String.format(java.util.Locale.ROOT, "coarse height %.1f m", coarseHeight(x + 0.5, z + 0.5));
        String[] types = {"dry", "main", "creek", "lake", "oxbow", "pool"};
        String[] banks = {"none", "bar", "cut_bank", "bog", "weed_shelf", "drop_off"};
        return String.format(java.util.Locale.ROOT,
                "height %.2f m, water %s%s, bank %s, rock %d, sediment %.1f m, water %.0f m away, snow class %d%s",
                g.sample(ground, x + 0.5 - g.originX, z + 0.5 - g.originZ), types[wType[cell]],
                wType[cell] != 0 ? String.format(java.util.Locale.ROOT, " at %.2f m", ws[cell]) : "", banks[bank[cell]], rock[cell],
                (sediment[cell] & 0xFF) * 0.1, Math.min(9999, cover.waterDist()[cell]), cover.snowClass(cell),
                fallsZone[cell] ? ", falls" : "");
    }

    /** The nearest core block column of a water type (WaterStage.MAIN, CREEK, ...) within r blocks, or null. */
    /** The water layer value (WaterStage.DRY/MAIN/CREEK/LAKE/OXBOW/POOL) at a block column, DRY outside the core. */
    public int waterTypeAt(int x, int z) {
        int c = cellOf(x, z);
        return c < 0 ? WaterStage.DRY : wType[c];
    }

    /** The water flags (WaterStage.F_THIN, F_SPRING, F_LEAD, ...) at a block column, 0 outside the core. */
    public int waterFlagsAt(int x, int z) {
        int c = cellOf(x, z);
        return c < 0 ? 0 : wFlags[c] & 0xFF;
    }

    public int[] nearestWater(int x, int z, int r, int type) {
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int dz = -r; dz <= r; dz += 2) for (int dx = -r; dx <= r; dx += 2) {
            int cell = cellOf(x + dx, z + dz);
            if (cell < 0 || wType[cell] != type) continue;
            double d = dx * (double) dx + dz * (double) dz;
            if (d < bd) { bd = d; best = new int[] {x + dx, z + dz}; }
        }
        return best;
    }

    /** One block column inside each iron node cell, as {x, z}. */
    public List<int[]> ironNodes() {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < node.length; i++) {
            if (node[i] == NODE_IRON) out.add(new int[] {(int) Math.floor(g.worldX(i)), (int) Math.floor(g.worldZ(i))});
        }
        return out;
    }

    /** The highest ground within r blocks (on a 4-block grid), as {x, z}. */
    public int[] highPoint(int x, int z, int r) {
        int[] best = {x, z};
        double bh = -Double.MAX_VALUE;
        for (int dz = -r; dz <= r; dz += 4) for (int dx = -r; dx <= r; dx += 4) {
            double h = groundAt(x + dx + 0.5, z + dz + 0.5);
            if (h > bh) { bh = h; best = new int[] {x + dx, z + dz}; }
        }
        return best;
    }

    /** Fishing spots within a radius of a block position, nearest first (FishSpotService). */
    public List<WaterStage.Spot> spotsNear(double x, double z, double radius) {
        List<WaterStage.Spot> out = new ArrayList<>();
        for (WaterStage.Spot s : spots) {
            double dx = s.x() - x, dz = s.z() - z;
            if (dx * dx + dz * dz <= (radius + s.radiusM()) * (radius + s.radiusM())) out.add(s);
        }
        if (nearSpot != null && d2(nearSpot, x, z) <= (radius + nearSpot.radiusM()) * (radius + nearSpot.radiusM())) out.add(nearSpot);
        out.sort((a, b) -> Double.compare(d2(a, x, z), d2(b, x, z)));
        return out;
    }

    private static double d2(WaterStage.Spot s, double x, double z) {
        double dx = s.x() - x, dz = s.z() - z;
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------------ cover anywhere

    private final ThreadLocal<Cover.Sample> sample = ThreadLocal.withInitial(Cover.Sample::new);

    /** Cover class index at a block column (core layer, or the coarse rules beyond it). */
    public int classAt(int x, int z) {
        int cell = cellOf(x, z);
        if (cell >= 0) return cover.classAt(cell);
        return coarseClass(x + 0.5, z + 0.5);
    }

    int coarseClass(double wx, double wz) {
        double e = coarseHeight(wx, wz);
        double step = p.grid().coarseCellM();
        double gx = (coarseHeight(wx + step, wz) - coarseHeight(wx - step, wz)) / (2 * step);
        double gz = (coarseHeight(wx, wz + step) - coarseHeight(wx, wz - step)) / (2 * step);
        double slope = CoreGrid.deg(Math.sqrt(gx * gx + gz * gz));
        double aspect = CoreGrid.bearing(-gx, -gz);
        double patch = Cover.patch(p.cover(), cover.patchSeed(), wx, wz);
        return Cover.classifyCoarse(p.cover(), sample.get(), e, slope, aspect, patch);
    }

    /** True inside a coarse coal bluff. */
    boolean inBluff(double wx, double wz) {
        for (CoalSeams.Seam b : bluffs) {
            double dx = wx - b.x(), dz = wz - b.z();
            if (dx * dx + dz * dz <= (b.radiusM() + 1) * (b.radiusM() + 1)) return true;
        }
        return false;
    }

    public Cover.ClassDef classDef(int index) { return p.cover().classes().get(index); }

    public String biomeAt(int x, int z) { return classDef(classAt(x, z)).biome(); }

    /** Coarse height with the edge-fading detail noise: the ground beyond the core. */
    double coarseHeight(double wx, double wz) {
        WorldgenParams.Grid gr = p.grid();
        double half = gr.worldSizeM() / 2.0 - 1;
        double base = Landform.coarseAt(coarse, cn, gr, Math.max(-half, Math.min(half, wx)), Math.max(-half, Math.min(half, wz)));
        double lx = wx - g.originX, lz = wz - g.originZ;
        double dx = lx < 0 ? -lx : (lx > g.size ? lx - g.size : 0), dz = lz < 0 ? -lz : (lz > g.size ? lz - g.size : 0);
        double out = Math.max(dx, dz);
        double fade = Noise.smooth(Noise.clamp01(out / gr.edgeBlendM()));
        Cover.Params cp = p.cover();
        return base + fade * cp.coarseDetailReliefM() * Noise.fbm(detailSeed, wx / cp.coarseDetailScaleM(), wz / cp.coarseDetailScaleM(), 3);
    }

    /** Ground height in metres at a world position, before rounding and footprints. */
    public double groundAt(double wx, double wz) {
        if (inCore(wx, wz)) return g.sample(ground, wx - g.originX, wz - g.originZ);
        return coarseHeight(wx, wz);
    }

    // ------------------------------------------------------------------ columns

    /** Column kinds. */
    public static final byte SOIL = 0, ROCK = 1, WATER = 2, LIP = 3;
    /** Column flags. */
    public static final int F_PERMAFROST = 1, F_FLAT = 2, F_FLOW = 4, F_IRON = 8, F_COAL = 16, F_RUST = 32, F_BANK = 64;
    public static final int NONE = Integer.MIN_VALUE;

    /**
     * The block columns of an area, as the chunk builder writes them (Terrain to blocks, steps 1–8). Arrays are
     * row-major over {@code w × d}, z then x.
     *
     * <ul>
     *   <li>{@code ground}: the surface block's y. Below it: {@code soil} blocks, then deep ground; or, on rock,
     *       face rock from {@code shellBottom} up and deep rock behind it.</li>
     *   <li>{@code water}: the top water block's y, or {@link #NONE}. Water fills from ground + 1.</li>
     *   <li>A lip ({@link #LIP}) is the lower column at a one-block step of the water surface: gravel up to
     *       {@code lip − 1} and a riffle cobble at {@code lip}, so the upper water never meets air at its own
     *       level. Steps at falls are left open ({@link #F_FLOW}) and the water falls, which is the waterfall.</li>
     * </ul>
     */
    public static final class Columns {
        public final int x0, z0, w, d;
        public final int[] ground, water, lip, shellBottom, coalY;
        public final byte[] kind, cover;
        public final short[] surface, soil, lower;
        public final byte[] flags;

        Columns(int x0, int z0, int w, int d) {
            this.x0 = x0;
            this.z0 = z0;
            this.w = w;
            this.d = d;
            int t = w * d;
            ground = new int[t];
            water = new int[t];
            lip = new int[t];
            shellBottom = new int[t];
            coalY = new int[t];
            kind = new byte[t];
            cover = new byte[t];
            surface = new short[t];
            soil = new short[t];
            lower = new short[t];
            flags = new byte[t];
        }

        public int at(int x, int z) { return (z - z0) * w + (x - x0); }

        public boolean contains(int x, int z) { return x >= x0 && z >= z0 && x < x0 + w && z < z0 + d; }

        public boolean has(int i, int f) { return (flags[i] & f) != 0; }

        /** Highest non-air block of a column: water, lip cobble or ground. */
        public int top(int i) {
            if (kind[i] == WATER) return water[i];
            if (kind[i] == LIP) return lip[i];
            return ground[i];
        }
    }

    /** Raw values of one column before the lip and bank passes. */
    private static final class Raw {
        int ground, water, cls, cell;
        byte kind;
        boolean flat, perma, falls, iron, coal, rust, cut;
        int coalBand;
    }

    public Columns columns(int x0, int z0, int w, int d) {
        int m = 2;
        int rw = w + 2 * m, rd = d + 2 * m;
        Raw[] raw = new Raw[rw * rd];
        List<int[]> pads = padsNear(x0 - m - 8, z0 - m - 8, x0 + w + m + 8, z0 + d + m + 8);
        for (int k = 0; k < rd; k++) for (int i = 0; i < rw; i++) raw[k * rw + i] = raw(x0 - m + i, z0 - m + k, pads);

        // Finalise the requested area plus one ring: lips and bank raises read raw 4-neighbours only.
        int fw = w + 2, fd = d + 2;
        int[] top = new int[fw * fd];
        int[] gnd = new int[fw * fd];
        int[] lipAt = new int[fw * fd];
        byte[] kind = new byte[fw * fd];
        boolean[] bankRaised = new boolean[fw * fd];
        for (int k = 0; k < fd; k++) for (int i = 0; i < fw; i++) {
            Raw c = raw[(k + 1) * rw + i + 1];
            int g0 = c.ground, l = NONE;
            byte kd = c.kind;
            boolean raised = false;
            for (int dir = 0; dir < 4; dir++) {
                Raw nb = raw[(k + 1 + DZ4[dir]) * rw + i + 1 + DX4[dir]];
                if (nb.kind != WATER) continue;
                if (kd == WATER) {
                    if (nb.water > c.water && !((c.falls || nb.falls) && nb.water - c.water >= 2)) l = Math.max(l, nb.water);
                } else if (nb.water > g0) {
                    g0 = nb.water; // bank raise: the dry neighbour of water is never lower than that water
                    raised = true;
                }
            }
            if (kd == WATER && l != NONE) kd = LIP;
            int f = k * fw + i;
            kind[f] = kd;
            gnd[f] = kd == LIP ? l - 1 : g0;
            lipAt[f] = kd == LIP ? l : NONE;
            top[f] = kd == WATER ? c.water : (kd == LIP ? l : g0);
            bankRaised[f] = raised;
        }

        Columns out = new Columns(x0, z0, w, d);
        int soilBlocks = p.terrain().soilBlocks();
        for (int k = 0; k < d; k++) for (int i = 0; i < w; i++) {
            Raw c = raw[(k + 2) * rw + i + 2];
            int f = (k + 1) * fw + i + 1;
            int o = k * w + i;
            out.kind[o] = kind[f];
            out.ground[o] = gnd[f];
            out.water[o] = kind[f] == WATER ? c.water : NONE;
            out.lip[o] = lipAt[f];
            out.cover[o] = (byte) c.cls;
            int fl = 0;
            if (c.perma) fl |= F_PERMAFROST;
            if (c.flat) fl |= F_FLAT;
            if (bankRaised[f]) fl |= F_BANK;
            if (kind[f] == WATER) {
                // Open steps at falls: a lower water neighbour that was let off its lip (see the lip rule above).
                for (int dir = 0; dir < 4; dir++) {
                    Raw nb = raw[(k + 2 + DZ4[dir]) * rw + i + 2 + DX4[dir]];
                    if (nb.kind == WATER && c.water - nb.water >= 2 && (c.falls || nb.falls)) fl |= F_FLOW;
                }
            }
            int surf = kind[f] == WATER || kind[f] == LIP ? classBed[c.cls] : classSurface[c.cls];
            int soil = classSoil[c.cls];
            if (kind[f] == LIP) surf = TerrainPalette.GRAVEL;
            if (c.cut && kind[f] == SOIL) soil = clayBank;
            if (c.rust && kind[f] == SOIL) { surf = TerrainPalette.RUST_STAIN; fl |= F_RUST; }
            if (kind[f] == ROCK) {
                surf = TerrainPalette.FACE_ROCK;
                int minNb = Integer.MAX_VALUE;
                for (int dir = 0; dir < 4; dir++) minNb = Math.min(minNb, top[(k + 1 + DZ4[dir]) * fw + i + 1 + DX4[dir]]);
                out.shellBottom[o] = Math.min(gnd[f], minNb + 1);
                if (c.iron) fl |= F_IRON;
                if (c.coal) { fl |= F_COAL; out.coalY[o] = Math.max(out.shellBottom[o], Math.min(gnd[f], c.coalBand)); }
            }
            out.surface[o] = (short) surf;
            out.soil[o] = (short) soil;
            out.lower[o] = (short) (c.perma && kind[f] == SOIL && soilBlocks >= 2 ? TerrainPalette.FROZEN_SOIL : soil);
            out.flags[o] = (byte) fl;
        }
        return out;
    }

    static final int[] DX4 = {1, 0, -1, 0}, DZ4 = {0, 1, 0, -1};

    /** Footprint rectangles (with their skirt) that touch an area: x0, z0, x1 (exclusive), z1, padY. */
    private List<int[]> padsNear(int ax, int az, int bx, int bz) {
        int skirt = (int) Math.ceil(p.terrain().footprintSkirtM());
        List<int[]> out = new ArrayList<>();
        for (SetPieces.Placement q : placements) {
            int[] r = rect(q);
            if (r[0] + r[2] + skirt < ax || r[0] - skirt > bx || r[1] + r[3] + skirt < az || r[1] - skirt > bz) continue;
            out.add(new int[] {r[0], r[1], r[0] + r[2], r[1] + r[3], padY(q)});
        }
        return out;
    }

    /** Distance outside a pad rectangle in blocks (0 inside). */
    private static double padDist(int[] pad, int x, int z) {
        int dx = x < pad[0] ? pad[0] - x : (x >= pad[2] ? x - pad[2] + 1 : 0);
        int dz = z < pad[1] ? pad[1] - z : (z >= pad[3] ? z - pad[3] + 1 : 0);
        return Math.max(dx, dz);
    }

    private Raw raw(int x, int z, List<int[]> pads) {
        Raw r = new Raw();
        double wx = x + 0.5, wz = z + 0.5;
        int cell = cellOf(x, z);
        r.cell = cell;
        double h;
        boolean wet = false;
        double surf = 0;
        if (cell >= 0) {
            h = g.sample(ground, wx - g.originX, wz - g.originZ);
            r.cls = cover.classAt(cell);
            r.perma = cover.permafrost(cell);
            int t = wType[cell];
            if (t != WaterStage.DRY) { wet = true; surf = ws[cell]; }
            r.falls = fallsZone[cell];
            int rk = rock[cell];
            r.kind = !wet && rk != 0 && rk != WaterStage.RUST_STAIN ? ROCK : SOIL;
            r.iron = node[cell] == NODE_IRON;
            r.coal = node[cell] == NODE_COAL;
            r.coalBand = coalBand[cell];
            r.rust = rk == WaterStage.RUST_STAIN;
            r.cut = bank[cell] == WaterStage.CUT_BANK;
        } else {
            h = coarseHeight(wx, wz);
            r.cls = coarseClass(wx, wz);
            Cover.ClassDef d = classDef(r.cls);
            r.kind = Boolean.TRUE.equals(d.rule().rock()) ? ROCK : SOIL;
            r.perma = d.permafrost() == Cover.Permafrost.YES;
            for (CoalSeams.Seam b : bluffs) {
                double dx = wx - b.x(), dz = wz - b.z();
                double dd = Math.sqrt(dx * dx + dz * dz);
                if (dd > b.radiusM()) continue;
                h = Math.max(h, b.bandY() - 1 + CoalSeams.BLUFF_RISE_M * Noise.smooth(1 - dd / b.radiusM()) + 0.5);
                r.kind = ROCK;
                r.coal = true;
                r.coalBand = b.bandY();
                r.cls = rockClass;
            }
        }
        // Footprints: flat pads, blended into the ground over the skirt.
        double skirt = p.terrain().footprintSkirtM();
        for (int[] pad : pads) {
            double dd = padDist(pad, x, z);
            if (dd == 0) {
                h = pad[4];
                r.flat = true;
                wet = false;
                r.kind = SOIL;
                break;
            }
            if (dd <= skirt) {
                double t = Noise.smooth(dd / (skirt + 1));
                h = pad[4] + (h - pad[4]) * t;
                if (wet && h > surf) wet = false;
            }
        }
        r.ground = (int) Math.floor(h + 0.5);
        if (wet) {
            r.kind = WATER;
            r.water = (int) Math.floor(surf + 0.5);
            if (r.water <= r.ground) r.ground = r.water - 1; // the bed carve: every wet column holds water
            WaterStage.Spot sp = sliceSpot;
            if (sp != null) {
                double dx = wx - sp.x(), dz = wz - sp.z();
                if (dx * dx + dz * dz <= SPOT_POOL_RADIUS * SPOT_POOL_RADIUS) r.ground = Math.min(r.ground, r.water - SPOT_POOL_BLOCKS);
            }
        }
        return r;
    }
}
