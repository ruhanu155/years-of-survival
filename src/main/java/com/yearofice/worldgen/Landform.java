package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;

/**
 * Stage 2, Landform ({@code stageName "landform"}). Writes the core's height (float32) and hardness (u8)
 * at 2 m cells, plus the 32 m coarse height of the whole 8,192 m world.
 *
 * <p>Height = valley floor along the river path + a trough around the path (Terrain and hydrology:
 * {@code z = -h_t (1 - (d / (w/2))^2)}, with a flat bottom for the archetype's flat fraction) + base fBm
 * hills + ridged mountains masked to the archetype's north fraction (feathered 300 m), all domain-warped.
 * Noise is stretched north–south so ridges and side valleys run down to the main valley. Archetype
 * shaping adds the lake basin, or the canyon reach with walls and rock steps. The core's last 64 m blend
 * to the coarse field. Only squares, {@code + − × ÷} and {@code sqrt} are used.
 */
public final class Landform {
    public static final String STAGE = "landform";

    private Landform() {}

    /** Everything stage 2 drew, kept for later stages and the debug map. */
    public record Shape(double floorWidthM, double floorGradientPct, double riverSwingM, double troughDepthM,
                        Basin basin, Canyon canyon) {
        public record Basin(double centerX, double centerZ, double lengthM, double widthM, double depthM,
                            double rimM) {}

        public record Canyon(double reachStart, double reachEnd, double floorWidthM, double wallHeightM,
                             double[] stepAt, double[] stepHeightM) {}
    }

    /** Stage 2 output. Arrays are row-major, z then x, in real (mirrored) orientation, north row first. */
    public record Layers(int cells, float[] height, byte[] hardness, byte[] floorMask, int coarseCells,
                         float[] coarse, RiverPath path, Shape shape, boolean mirror) {
        public int heightHash() { return StageHash.of(height); }

        public int hardnessHash() { return StageHash.of(hardness); }

        public int coarseHash() { return StageHash.of(coarse); }

        /** The stage 2 fingerprint: height, hardness and coarse field together. */
        public int hash() {
            return StageHash.of(Integer.toUnsignedString(heightHash()) + ":" + Integer.toUnsignedString(hardnessHash())
                    + ":" + Integer.toUnsignedString(coarseHash()));
        }

        public double hardnessAt(int idx) { return (hardness[idx] & 0xFF) / 255.0; }
    }

    /** Noise seeds, drawn first and in a fixed order so archetype draws never shift them. */
    private record Seeds(int warpX, int warpZ, int base, int ridged, int foothill, int detail, int hard,
                         int pathLong, int pathShort, int floor) {}

    public static Layers run(SeedStage.SeedParams seed, WorldgenParams p, Archetype.Params[] archetypes) {
        Mulberry32 rng = SeedStage.rng(seed.attemptSeed(), STAGE);
        Seeds s = new Seeds((int) rng.nextUInt(), (int) rng.nextUInt(), (int) rng.nextUInt(), (int) rng.nextUInt(),
                (int) rng.nextUInt(), (int) rng.nextUInt(), (int) rng.nextUInt(), (int) rng.nextUInt(),
                (int) rng.nextUInt(), (int) rng.nextUInt());
        Archetype.Params a = archetypes[seed.archetype().ordinal()];
        double L = p.grid().coreSizeM();

        double floorWidth = a.floorWidthM().draw(rng);
        double gradient = a.floorGradientPct().draw(rng);
        double swing = a.riverSwingM().draw(rng);
        double troughDepth = a.troughDepthM().draw(rng);

        // The river path, canonical frame.
        RiverPath path = new RiverPath(L, seed.entryOffsetM(), seed.exitOffsetM(), swing, s.pathLong(), s.pathShort(),
                p.riverPath());

        Shape.Basin basin = null;
        if (a.basin() != null) {
            Archetype.Params.Basin b = a.basin();
            double len = b.lengthM().draw(rng), wid = b.widthM().draw(rng), depth = b.depthM().draw(rng);
            double along = L * (0.3 + 0.4 * rng.nextDouble());
            double centreZ = path.zAt(along) - b.riverGapM() - wid / 2;
            basin = new Shape.Basin(along, centreZ, len, wid, depth, 0); // rim set below from the floor
        }
        Shape.Canyon canyon = null;
        if (a.canyon() != null) {
            Archetype.Params.Canyon c = a.canyon();
            double reach = Math.min(c.reachM().draw(rng), c.maxReachFraction() * L);
            double start = L * 0.15 + (L * 0.7 - reach) * rng.nextDouble();
            double cw = c.floorWidthM().draw(rng);
            double wall = c.wallHeightM().draw(rng);
            int steps = c.steps().drawInt(rng);
            double[] at = new double[steps], h = new double[steps];
            double margin = 60;
            for (int k = 0; k < steps; k++) {
                // Even slots with a seeded jitter keep steps apart without rejection loops.
                double slot = (reach - 2 * margin) / steps;
                at[k] = start + margin + slot * (k + 0.2 + 0.6 * rng.nextDouble());
                h[k] = c.stepHeightM().draw(rng);
            }
            // The whole floor drop (gradient + steps) must keep the floor near 150 m.
            double sum = 0;
            for (double v : h) sum += v;
            double allowed = p.riverPath().maxTotalDropM() - gradient / 100.0 * L;
            if (sum > allowed && allowed > 0) {
                double f = allowed / sum;
                for (int k = 0; k < steps; k++) h[k] = Math.max(c.stepHeightM().min(), h[k] * f);
            }
            canyon = new Shape.Canyon(start, start + reach, cw, wall, at, h);
            floorWidth = c.outsideFloorWidthM();
        }
        Field f = new Field(p, a, s, path, floorWidth, gradient, troughDepth, basin, canyon);
        if (basin != null) {
            basin = new Shape.Basin(basin.centerX(), basin.centerZ(), basin.lengthM(), basin.widthM(), basin.depthM(),
                    f.floorSmooth(basin.centerX()) + 1.5);
            f = new Field(p, a, s, path, floorWidth, gradient, troughDepth, basin, canyon);
        }
        Shape shape = new Shape(floorWidth, gradient, swing, troughDepth, basin, canyon);

        // Coarse world field at 32 m.
        WorldgenParams.Grid g = p.grid();
        int cn = g.coarseCells();
        float[] coarse = new float[cn * cn];
        double half = g.worldSizeM() / 2.0;
        Sample sm = new Sample();
        for (int k = 0; k < cn; k++) {
            double wz = -half + (k + 0.5) * g.coarseCellM();
            for (int i = 0; i < cn; i++) {
                double wx = -half + (i + 0.5) * g.coarseCellM();
                double lx = wx - g.coreOriginXM(), lz = wz - g.coreOriginZM();
                f.sample(seed.mirror() ? L - lx : lx, lz, false, sm);
                coarse[k * cn + i] = (float) sm.height;
            }
        }

        // The core at 2 m.
        int n = g.coreCells();
        float[] height = new float[n * n];
        byte[] hardness = new byte[n * n];
        byte[] floorMask = new byte[n * n];
        double cell = g.cellM();
        for (int k = 0; k < n; k++) {
            double lz = (k + 0.5) * cell;
            for (int i = 0; i < n; i++) {
                double lx = (i + 0.5) * cell;
                f.sample(seed.mirror() ? L - lx : lx, lz, true, sm);
                double h = sm.height;
                // Blend to the coarse field over the last 64 m of the core.
                double edge = Math.min(Math.min(lx, L - lx), Math.min(lz, L - lz));
                if (edge < g.edgeBlendM()) {
                    double c = coarseAt(coarse, cn, g, g.coreOriginXM() + lx, g.coreOriginZM() + lz);
                    h = c + (h - c) * Noise.smooth(edge / g.edgeBlendM());
                }
                int idx = k * n + i;
                height[idx] = (float) h;
                hardness[idx] = (byte) (int) (Noise.clamp01(sm.hardness) * 255.0 + 0.5);
                floorMask[idx] = (byte) (sm.floor ? 1 : 0);
            }
        }
        return new Layers(n, height, hardness, floorMask, cn, coarse, path, shape, seed.mirror());
    }

    /** Bilinear coarse height at a world position (cell-centred samples). */
    public static double coarseAt(float[] coarse, int cn, WorldgenParams.Grid g, double wx, double wz) {
        double half = g.worldSizeM() / 2.0;
        double fx = (wx + half) / g.coarseCellM() - 0.5, fz = (wz + half) / g.coarseCellM() - 0.5;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        x0 = Math.max(0, Math.min(cn - 2, x0));
        z0 = Math.max(0, Math.min(cn - 2, z0));
        tx = Noise.clamp01(tx);
        tz = Noise.clamp01(tz);
        double a = coarse[z0 * cn + x0], b = coarse[z0 * cn + x0 + 1];
        double c = coarse[(z0 + 1) * cn + x0], d = coarse[(z0 + 1) * cn + x0 + 1];
        double top = a + (b - a) * tx, bot = c + (d - c) * tx;
        return top + (bot - top) * tz;
    }

    /** One sample of the field. */
    static final class Sample {
        double height, hardness;
        boolean floor;
    }

    /** The continuous landform field in the canonical frame. Stateless apart from its inputs. */
    static final class Field {
        final WorldgenParams p;
        final Archetype.Params a;
        final Seeds s;
        final RiverPath path;
        final double floorWidth, gradient, troughDepth, L, drop;
        final Shape.Basin basin;
        final Shape.Canyon canyon;

        Field(WorldgenParams p, Archetype.Params a, Seeds s, RiverPath path, double floorWidth, double gradient,
              double troughDepth, Shape.Basin basin, Shape.Canyon canyon) {
            this.p = p; this.a = a; this.s = s; this.path = path; this.floorWidth = floorWidth;
            this.gradient = gradient; this.troughDepth = troughDepth; this.basin = basin; this.canyon = canyon;
            this.L = p.grid().coreSizeM();
            double stepSum = 0;
            if (canyon != null) for (double v : canyon.stepHeightM()) stepSum += v;
            this.drop = gradient / 100.0 * L + stepSum;
        }

        /** Floor elevation without steps: linear from entry to exit, 150 m at mid-core. */
        double floorSmooth(double along) { return p.elevation().floorM() + drop * (0.5 - along / L); }

        /** Floor elevation with the canyon's rock steps: the drop is the gradient plus each step. */
        double floorStepped(double along) {
            if (canyon == null) return floorSmooth(along);
            double h = p.elevation().floorM() + gradient / 100.0 * L * (0.5 - along / L);
            double total = 0;
            for (double v : canyon.stepHeightM()) total += v;
            h += total / 2;
            double[] at = canyon.stepAt(), sh = canyon.stepHeightM();
            for (int k = 0; k < at.length; k++) h -= sh[k] * Noise.smooth((along - at[k]) / 6.0 + 0.5);
            return h;
        }

        /**
         * 1 inside the canyon reach, 0 outside. Keyed on canonical x so it is continuous; the ramp is 100 m at the
         * lip and widens to 400 m with distance from the river, so the rim tapers into the hills, not a square end.
         */
        double reachMask(double x, double d) {
            if (canyon == null) return 0;
            double ramp = 100.0 + Noise.clamp(d, 0, 300);
            return Noise.smooth((x - canyon.reachStart() + ramp / 2 - 50) / ramp)
                    * Noise.smooth((canyon.reachEnd() - x + ramp / 2 - 50) / ramp);
        }

        void sample(double x, double z, boolean detail, Sample out) {
            WorldgenParams.Warp w = p.domainWarp();
            double wx = x + w.amplitudeM() * Noise.fbm(s.warpX(), x / w.scaleM(), z / w.scaleM(), w.octaves());
            double wz = z + w.amplitudeM() * Noise.fbm(s.warpZ(), x / w.scaleM(), z / w.scaleM(), w.octaves());

            double d = path.distance(x, z);
            double reach = reachMask(x, d);
            double width = canyon == null ? floorWidth : Noise.lerp(floorWidth, canyon.floorWidthM(), reach);
            double half = width / 2;
            double flat = a.troughFlatFraction();

            // Floor: keyed on canonical x, not on the nearest path point, which jumps across the medial axis of
            // a bend and would leave seams. On the path the two are equal. Canyon steps stay inside the trough.
            double inner = 1 - Noise.smooth((d - half) / 80.0);
            double floor = Noise.lerp(floorSmooth(x), floorStepped(x), inner);
            double shoulder = floor + troughDepth;

            double trough = 0;
            if (d < half) {
                double t = (d - flat * half) / ((1 - flat) * half);
                if (t < 0) t = 0;
                trough = -troughDepth * (1 - t * t);
            }
            double e = d > half ? d - half : 0;
            double rise = p.wallRiseDistanceM();
            double env = e / (e + rise);

            WorldgenParams.NoiseLayer bn = p.baseNoise(), rn = p.ridgedNoise(), dn = p.detailNoise();
            double base = Noise.fbm(s.base(), wx / bn.scaleM(), wz / (bn.scaleM() * bn.stretchNorthSouth()),
                    detail ? bn.octaves() : bn.octaves() - 2);
            double foot = Noise.ridged(s.foothill(), wx / (bn.scaleM() * 0.75),
                    wz / (bn.scaleM() * 0.75 * bn.stretchNorthSouth()), 3);
            double hills = bn.reliefM() * env * (0.15 + 0.35 * (0.5 + 0.5 * base) + 0.5 * foot);

            double boundary = a.mountainFraction() * L;
            double mask = 1 - Noise.smooth((z - (boundary - 150)) / 300.0);
            double mountains = 0, ridge = 0;
            if (mask > 0) {
                ridge = Noise.ridged(s.ridged(), wx / rn.scaleM(), wz / (rn.scaleM() * rn.stretchNorthSouth()),
                        detail ? rn.octaves() : rn.octaves() - 1);
                double menv = e / (e + rise * 1.6);
                mountains = mask * menv * (rn.reliefM() * ridge + 0.25 * rn.reliefM());
            }

            double fine = 0;
            if (detail) {
                double fn = Noise.fbm(s.detail(), wx / dn.scaleM(), wz / dn.scaleM(), dn.octaves());
                double flatZone = d < flat * half ? 1 : 0;
                double floorNoise = a.floorNoiseM() * Noise.fbm(s.floor(), x / 90.0, z / 90.0, 2);
                fine = flatZone * floorNoise + (1 - flatZone) * fn * dn.reliefM() * (0.25 + 0.75 * env);
            }

            double walls = 0;
            if (canyon != null && reach > 0) {
                // The rim stands wall-height above the floor at the lip and settles into the hills 200–600 m out.
                walls = canyon.wallHeightM() * reach * Noise.smooth((d - half) / a.canyon().wallRunM())
                        * (1 - 0.7 * Noise.smooth((d - half - 200) / 400.0));
            }

            double h = shoulder + trough + hills + mountains + walls + fine;

            if (basin != null) {
                double rx = (x - basin.centerX()) / (basin.lengthM() / 2), rz = (z - basin.centerZ()) / (basin.widthM() / 2);
                double r2 = rx * rx + rz * rz;
                if (r2 < 1) {
                    double r = Math.sqrt(r2);
                    double bottom = basin.rimM() - basin.depthM() * (1 - r2);
                    double carved = Noise.lerp(bottom, h, Noise.smooth((r - 0.7) / 0.3));
                    if (carved < h) h = carved;
                }
            }

            // Soft limits: the core stays inside 120–900 m; outside the core the cap opens to the range.
            WorldgenParams.Elevation el = p.elevation();
            double outside = Math.max(Math.max(-x, x - L), Math.max(-z, z - L));
            double open = outside > 0 ? Noise.smooth(outside / 1500.0) : 0;
            double knee = Noise.lerp(el.softCapKneeM(), el.worldCapM() - 200, open);
            double cap = Noise.lerp(el.softCapM(), el.worldCapM(), open);
            if (h > knee) {
                double q = (h - knee) / (cap - knee);
                h = knee + (cap - knee) * q / (1 + q);
            }
            if (h < el.softFloorKneeM()) {
                double span = el.softFloorKneeM() - el.softFloorM();
                double q = (el.softFloorKneeM() - h) / span;
                h = el.softFloorKneeM() - span * q / (1 + q);
            }
            out.height = h;

            WorldgenParams.Hardness hp = p.hardness();
            double hn = Noise.fbm(s.hard(), x / hp.scaleM(), z / hp.scaleM(), hp.octaves());
            double hard = hp.base() + hp.noise() * hn + hp.ridgedBoost() * mask * ridge * (0.3 + 0.7 * env);
            if (canyon != null && reach > 0) {
                double band = Noise.smooth((d - half + 6) / 6.0) * (1 - Noise.smooth((d - half - a.canyon().wallRunM() - 30) / 40.0));
                hard += hp.canyonWallBoost() * reach * band;
            }
            out.hardness = hard;
            out.floor = d < Math.max(flat, 0.5) * half;
        }
    }
}
