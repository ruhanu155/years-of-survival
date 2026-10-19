package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;

/**
 * Stage 3, Erosion ({@code stageName "erosion"}). Droplet hydraulic erosion, then thermal erosion at a
 * 35° talus (1.4 m per 2 m cell) on soft cells. Writes eroded height (replaces stage 2's, in a new array)
 * and sediment depth (u8, 0.1 m steps). Reads stage 2's layers and never writes them, so the stage 2 hash
 * cannot depend on any number here.
 *
 * <p>Hard cells (hardness over 0.6) skip thermal erosion and erode at 25% of the droplet rate, so the north
 * range keeps its faces. Droplets carry their sediment to the end of their life and drop it there, so
 * eroded and deposited volume balance. Erosion fades out over the core's last 64 m to keep the coarse seam.
 * Sequential by design: droplet order is part of the result.
 */
public final class Erosion {
    public static final String STAGE = "erosion";

    private Erosion() {}

    public record Layers(int cells, float[] height, byte[] sediment, double erodedM3, double depositedM3,
                         double thermalMovedM3) {
        public int heightHash() { return StageHash.of(height); }

        public int sedimentHash() { return StageHash.of(sediment); }

        public int hash() {
            return StageHash.of(Integer.toUnsignedString(heightHash()) + ":" + Integer.toUnsignedString(sedimentHash()));
        }
    }

    public static Layers run(SeedStage.SeedParams seed, Landform.Layers land, WorldgenParams p, Archetype.Params a) {
        WorldgenParams.ErosionParams e = p.erosion();
        int n = land.cells();
        double cell = p.grid().cellM();
        float[] h = land.height().clone();

        // Per-cell erodibility: hard cells at 25%, faded to 0 at the core edge.
        float[] erodibility = new float[n * n];
        boolean[] thermalSkip = new boolean[n * n];
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                int idx = k * n + i;
                double edgeM = Math.min(Math.min(i, n - 1 - i), Math.min(k, n - 1 - k)) * cell;
                double fade = Noise.smooth(edgeM / e.edgeFadeM());
                boolean hard = land.hardnessAt(idx) > e.hardThreshold();
                erodibility[idx] = (float) (fade * (hard ? e.hardErodeFactor() : 1.0));
                thermalSkip[idx] = hard || fade < 1.0;
            }
        }

        // Brush: weights fall off linearly to the radius and sum to 1.
        int r = e.brushRadiusCells();
        int side = 2 * r + 1;
        int[] bdx = new int[side * side], bdz = new int[side * side];
        double[] bw = new double[side * side];
        int bc = 0;
        double wsum = 0;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < r) {
                    bdx[bc] = dx; bdz[bc] = dz; bw[bc] = r - dist; wsum += r - dist; bc++;
                }
            }
        }
        for (int b = 0; b < bc; b++) bw[b] /= wsum;

        double eroded = 0, deposited = 0;
        Mulberry32 rng = SeedStage.rng(seed.attemptSeed(), STAGE);
        double cellArea = cell * cell;
        for (int drop = 0; drop < e.droplets(); drop++) {
            double x = rng.nextDouble() * (n - 1);
            double z = rng.nextDouble() * (n - 1);
            double dirX = 0, dirZ = 0, speed = 1, water = 1, sediment = 0;
            int lastNode = -1;
            for (int step = 0; step < e.dropletLifetimeSteps(); step++) {
                int nx = (int) x, nz = (int) z;
                double u = x - nx, v = z - nz;
                int node = nz * n + nx;
                double h00 = h[node], h10 = h[node + 1], h01 = h[node + n], h11 = h[node + n + 1];
                double gx = (h10 - h00) * (1 - v) + (h11 - h01) * v;
                double gz = (h01 - h00) * (1 - u) + (h11 - h10) * u;
                double hNow = h00 * (1 - u) * (1 - v) + h10 * u * (1 - v) + h01 * (1 - u) * v + h11 * u * v;

                dirX = dirX * e.inertia() - gx * (1 - e.inertia());
                dirZ = dirZ * e.inertia() - gz * (1 - e.inertia());
                double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
                lastNode = node;
                if (len < 1e-12) break; // a perfectly flat cell: the droplet stops and drops its load
                dirX /= len;
                dirZ /= len;
                double x2 = x + dirX, z2 = z + dirZ;
                if (x2 < 0 || x2 >= n - 1 || z2 < 0 || z2 >= n - 1) break;

                double hNew = heightAt(h, n, x2, z2);
                double dh = hNew - hNow;
                double capacity = Math.max(-dh * speed * water * e.sedimentCapacity(), e.minSedimentCapacity());

                if (sediment > capacity || dh > 0) {
                    double put = dh > 0 ? Math.min(dh, sediment) : (sediment - capacity) * e.depositRate();
                    sediment -= put;
                    deposited += put;
                    deposit(h, n, node, u, v, put);
                } else {
                    double take = Math.min(Math.min((capacity - sediment) * e.erodeRate(), -dh), e.maxErodePerStepM());
                    int bx = nx, bz = nz;
                    for (int b = 0; b < bc; b++) {
                        int cx = bx + bdx[b], cz = bz + bdz[b];
                        if (cx < 0 || cz < 0 || cx >= n || cz >= n) continue;
                        int idx = cz * n + cx;
                        double amt = take * bw[b] * erodibility[idx];
                        if (amt <= 0) continue;
                        h[idx] = (float) (h[idx] - amt);
                        sediment += amt;
                        eroded += amt;
                    }
                }
                speed = Math.sqrt(Math.max(0, speed * speed - dh * e.gravity()));
                water *= 1 - e.evaporationPerStep();
                x = x2;
                z = z2;
            }
            if (sediment > 0 && lastNode >= 0) { // the droplet dies: its load stays on the land, spread by the brush
                int bx = lastNode % n, bz = lastNode / n;
                double laid = 0, wIn = 0;
                for (int b = 0; b < bc; b++) {
                    int cx = bx + bdx[b], cz = bz + bdz[b];
                    if (cx >= 0 && cz >= 0 && cx < n && cz < n) wIn += bw[b];
                }
                for (int b = 0; b < bc; b++) {
                    int cx = bx + bdx[b], cz = bz + bdz[b];
                    if (cx < 0 || cz < 0 || cx >= n || cz >= n) continue;
                    double amt = sediment * bw[b] / wIn;
                    h[cz * n + cx] = (float) (h[cz * n + cx] + amt);
                    laid += amt;
                }
                deposited += laid;
            }
        }

        double moved = thermal(h, n, thermalSkip, e);

        // Sediment depth: what erosion laid down, plus floor alluvium, times the archetype's floor multiplier.
        byte[] sed = new byte[n * n];
        float[] before = land.height();
        byte[] floor = land.floorMask();
        for (int idx = 0; idx < n * n; idx++) {
            double dep = h[idx] - before[idx];
            double s = dep > 0 ? dep : 0;
            if (floor[idx] != 0) s = (s + e.floorAlluviumM()) * a.floorSedimentMultiplier();
            int q = (int) (s * 10.0 + 0.5);
            sed[idx] = (byte) (q > 255 ? 255 : q);
        }
        return new Layers(n, h, sed, eroded * cellArea, deposited * cellArea, moved * cellArea);
    }

    private static double heightAt(float[] h, int n, double x, double z) {
        int nx = (int) x, nz = (int) z;
        double u = x - nx, v = z - nz;
        int node = nz * n + nx;
        return h[node] * (1 - u) * (1 - v) + h[node + 1] * u * (1 - v) + h[node + n] * (1 - u) * v
                + h[node + n + 1] * u * v;
    }

    private static void deposit(float[] h, int n, int node, double u, double v, double amount) {
        h[node] = (float) (h[node] + amount * (1 - u) * (1 - v));
        h[node + 1] = (float) (h[node + 1] + amount * u * (1 - v));
        h[node + n] = (float) (h[node + n] + amount * (1 - u) * v);
        h[node + n + 1] = (float) (h[node + n + 1] + amount * u * v);
    }

    private static final int[] NDX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] NDZ = {-1, -1, -1, 0, 0, 1, 1, 1};

    /**
     * Thermal erosion, Jacobi style so the result is order-free: each pass reads the old heights and moves
     * half the largest excess over the talus to the lower neighbours, in proportion to their excess.
     * Diagonal talus is scaled by sqrt(2). Returns the volume moved, in m·cells.
     */
    static double thermal(float[] h, int n, boolean[] skip, WorldgenParams.ErosionParams e) {
        double talus = e.talusDropPerCellM();
        double[] tal = new double[8];
        for (int k = 0; k < 8; k++) tal[k] = (NDX[k] != 0 && NDZ[k] != 0) ? talus * 1.4142135623730951 : talus;
        int[] off = new int[8];
        for (int k = 0; k < 8; k++) off[k] = NDZ[k] * n + NDX[k];
        float[] delta = new float[n * n];
        double[] ex = new double[8];
        double moved = 0;
        for (int pass = 0; pass < e.thermalPasses(); pass++) {
            boolean any = false;
            for (int z = 1; z < n - 1; z++) {
                int row = z * n;
                for (int x = 1; x < n - 1; x++) {
                    int idx = row + x;
                    if (skip[idx]) continue;
                    double hc = h[idx];
                    double total = 0, max = 0;
                    for (int k = 0; k < 8; k++) {
                        double d = hc - h[idx + off[k]] - tal[k];
                        if (d > 0) {
                            ex[k] = d; total += d;
                            if (d > max) max = d;
                        } else {
                            ex[k] = 0;
                        }
                    }
                    if (total <= 0) continue;
                    any = true;
                    double move = e.thermalMoveFraction() * max;
                    delta[idx] -= (float) move;
                    for (int k = 0; k < 8; k++) if (ex[k] > 0) delta[idx + off[k]] += (float) (move * ex[k] / total);
                    moved += move;
                }
            }
            if (!any) break;
            for (int i = 0; i < delta.length; i++) {
                if (delta[i] != 0) { h[i] += delta[i]; delta[i] = 0; }
            }
        }
        return moved;
    }
}
