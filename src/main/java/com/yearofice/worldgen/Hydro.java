package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4's working state while it runs: the layers it is writing and the lists it collects. One instance per
 * run, filled by {@link Rivers}, {@link Lakes}, {@link Falls}, {@link FishSpots} and {@link RockFaces} in the
 * build order of the M1 spec, then frozen into {@link WaterStage.Layers}. Never shared between threads.
 */
final class Hydro {
    // Water type layer.
    static final byte DRY = 0, MAIN = 1, CREEK = 2, LAKE = 3, OXBOW = 4, POOL = 5;
    // Bank and ground layer.
    static final byte NONE = 0, BAR = 1, CUT_BANK = 2, BOG = 3, WEED_SHELF = 4, DROP_OFF = 5;
    // Rock layer.
    static final byte FACE_ROCK = 1, IRON_OUTCROP = 2, RUST_STAIN = 3;
    // Flags, the current attachment's flag byte (Minecraft development › The current attachment).
    static final int F_THIN = 1, F_SPRING = 2, F_LEAD = 4, F_FALLS_POOL = 8, F_FORD = 16, F_RIFFLE = 32, F_SILTY = 64,
            F_SWEEPER = 128;
    static final int STILL = 255;

    final SeedStage.SeedParams seed;
    final Landform.Layers land;
    final Erosion.Layers ero;
    final WorldgenParams p;
    final HydroParams hp;
    final Archetype.Params arch;
    final CoreGrid grid;
    final int n, total;
    final double cellArea;
    final Mulberry32 rng;

    final float[] h0;            // eroded height (stage 3), read only
    final float[] carveMain;     // main channel and oxbow carve
    final float[] carveCreek;    // creek carve, rebuilt when the falls guarantee deepens a creek
    final float[] carvePool;     // plunge pools
    final float[] extra;         // the falls guarantee's deepening, applied before the creek surface
    final byte[] type;
    final float[] ws;
    final short[] body;
    final byte[] dir;
    final byte[] speed;
    final float[] catchment;
    final float[] width;
    final byte[] bank;
    final byte[] flags;
    final byte[] flood;
    final byte[] rock;
    final boolean[] oxbowCarved;

    CoarseRivers.Net coarse;
    Rivers.Main main;
    Lakes.Flood flood0;
    final List<WaterStage.Body> bodies = new ArrayList<>();
    final List<WaterStage.Lake> lakes = new ArrayList<>();
    final List<WaterStage.Creek> creeks = new ArrayList<>();
    final List<WaterStage.Fall> falls = new ArrayList<>();
    final List<WaterStage.Spring> springs = new ArrayList<>();
    final List<WaterStage.Spot> spots = new ArrayList<>();
    final List<WaterStage.Face> faces = new ArrayList<>();
    final List<WaterStage.IronCluster> iron = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    int[] creekOrder = new int[0]; // creek cells, upstream first
    int[] receiver;                // D8 receiver per cell, −1 at outlets

    Hydro(SeedStage.SeedParams seed, Landform.Layers land, Erosion.Layers ero, WorldgenParams p, Archetype.Params arch) {
        this.seed = seed;
        this.land = land;
        this.ero = ero;
        this.p = p;
        this.hp = p.hydro();
        this.arch = arch;
        this.grid = new CoreGrid(p, seed.mirror());
        this.n = grid.n;
        this.total = n * n;
        this.cellArea = grid.cell * grid.cell;
        this.rng = SeedStage.rng(seed.attemptSeed(), WaterStage.STAGE);
        this.h0 = ero.height();
        carveMain = new float[total];
        carveCreek = new float[total];
        carvePool = new float[total];
        extra = new float[total];
        type = new byte[total];
        ws = new float[total];
        body = new short[total];
        dir = new byte[total];
        java.util.Arrays.fill(dir, (byte) STILL);
        speed = new byte[total];
        catchment = new float[total];
        width = new float[total];
        bank = new byte[total];
        flags = new byte[total];
        flood = new byte[total];
        rock = new byte[total];
        oxbowCarved = new boolean[total];
    }

    /** Ground after every carve so far. */
    float[] ground() {
        float[] g = new float[total];
        for (int i = 0; i < total; i++) g[i] = h0[i] - carveMain[i] - carveCreek[i] - carvePool[i];
        return g;
    }

    boolean wet(int i) { return type[i] != DRY; }

    void flag(int i, int f) { flags[i] = (byte) (flags[i] | f); }

    boolean has(int i, int f) { return (flags[i] & f) != 0; }

    /** 64-step direction index of a vector (0 = east, 16 = south, 32 = west, 48 = north). */
    static int dirIndex(double dx, double dz) {
        double a = StrictMath.atan2(dz, dx);
        int k = (int) Math.round(a / (2 * StrictMath.PI) * 64.0);
        return ((k % 64) + 64) % 64;
    }

    static double dirX(int d) { return StrictMath.cos(d * 2 * StrictMath.PI / 64.0); }

    static double dirZ(int d) { return StrictMath.sin(d * 2 * StrictMath.PI / 64.0); }

    void setCurrent(int i, double dx, double dz, double v) {
        dir[i] = (byte) dirIndex(dx, dz);
        int q = (int) Math.round(v / 0.05);
        speed[i] = (byte) Math.max(0, Math.min(60, q));
    }

    double speedAt(int i) { return (speed[i] & 0xFF) * 0.05; }

    /** Water depth at a cell (0 on dry cells). */
    double depth(int i) {
        if (type[i] == DRY) return 0;
        return ws[i] - (h0[i] - carveMain[i] - carveCreek[i] - carvePool[i]);
    }
}
