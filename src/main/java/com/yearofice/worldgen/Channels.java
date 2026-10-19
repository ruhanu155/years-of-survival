package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * The main river's channel inside the stage 2 trough (Terrain and hydrology › River geometry, Braids, Oxbows):
 * meanders, oxbow cut-offs and braids, as polylines in the canonical frame (river flowing toward +x), sampled
 * every 2 m.
 *
 * <p><b>Meanders.</b> A sine-generated curve: the channel's heading off the valley path swings by
 * {@code ω · sin(2π s / λ + ψ(s))}, where λ is so many channel widths and ψ a slow seeded phase drift that
 * makes bends uneven. A restoring turn pulls the channel back toward the valley path so it stays on the flat
 * floor; near the core edges the meander fades and the channel is steered onto the coarse net's crossings.
 *
 * <p><b>Cut-offs.</b> Wherever two points more than 6 widths apart along the channel come within 1.5 widths,
 * the loop between them is cut off: the channel takes the short way and the loop is kept as an oxbow.
 *
 * <p><b>Braids.</b> Each braid follows the main line at a lateral offset of
 * {@code belt/2 · pinch(s) · (lane + wander(s))}; the pinch closes to near zero every 450–900 m, where the
 * channels rejoin, and opens between, where they split across the gravel belt.
 */
public final class Channels {
    private Channels() {}

    public static final double DS = 2.0;

    /** A polyline in canonical metres. */
    public record Line(double[] x, double[] z) {
        public int size() { return x.length; }
    }

    /** The main line and the loops cut off from it, in the order they were cut. */
    public record Meander(Line line, List<Line> cutLoops, double omega, double wavelengthM, int retries) {}

    /** Where the channel may wander: the floor's usable half-width and an optional minimum offset (lake basin). */
    public interface Envelope {
        /** Largest lateral offset from the valley path at canonical x, in metres. */
        double maxOffset(double x);

        /** Smallest allowed offset (south positive) at canonical x, or −∞. */
        double minOffset(double x);
    }

    /**
     * Walks the meandering channel from the entry crossing to the exit crossing.
     *
     * @param entryZ canonical z where the coarse net enters the core (at x = 0)
     * @param exitZ  canonical z where it leaves (at x = L)
     */
    public static Line walk(RiverPath path, double L, double entryZ, double exitZ, double omega, double lambda,
                            double restore, Envelope env, double edgePin, int phaseSeed, int ampSeed) {
        List<double[]> pts = new ArrayList<>();
        double x = 0, z = entryZ, s = 0;
        pts.add(new double[] {x, z});
        double oEntry = entryZ - path.zAt(0), oExit = exitZ - path.zAt(L);
        int maxSteps = (int) (10 * L / DS);
        double twoPi = 2 * StrictMath.PI;
        for (int step = 0; step < maxSteps && x < L - DS; step++) {
            double xc = Noise.clamp(x, 0, L);
            double pin = Noise.smooth(xc / edgePin) * Noise.smooth((L - xc) / edgePin);
            double pz = path.zAt(xc);
            double slope = (path.zAt(xc + 4) - path.zAt(xc - 4)) / 8.0;
            double thv = StrictMath.atan(slope);
            double oTarget = (xc < L / 2 ? oEntry : oExit) * (1 - pin);
            double raw = z - pz;
            double o = raw - oTarget;
            double om = Math.max(4.0, env.maxOffset(xc) * Math.max(pin, 0.12));
            double amp = omega * pin * (0.85 + 0.15 * Noise.noise1(ampSeed, s / (3 * lambda)));
            double phase = twoPi * s / lambda + StrictMath.PI * Noise.noise1(phaseSeed, s / (2.5 * lambda));
            double phi = amp * StrictMath.sin(phase);
            double pull = restore * o / om;
            double ao = Math.abs(o);
            if (ao > om) pull += Math.signum(o) * 2.0 * (ao - om) / om;
            phi -= Noise.clamp(pull, -1.3, 1.3);
            double omin = env.minOffset(xc);
            if (raw < omin) phi += Noise.clamp(2.0 * (omin - raw) / 20.0, 0, 1.3);
            double th;
            if (L - x < 60) {
                th = StrictMath.atan2(exitZ - z, L - x); // steer onto the exit crossing
            } else {
                th = thv + phi;
            }
            x += DS * StrictMath.cos(th);
            z += DS * StrictMath.sin(th);
            s += DS;
            pts.add(new double[] {x, z});
        }
        pts.add(new double[] {L, exitZ});
        return resample(toLine(pts), DS);
    }

    /** Channel width at a canonical position. */
    public interface WidthFn { double width(double x, double z); }

    /**
     * Repeatedly cuts the first neck where points more than {@code minSepWidths} widths apart along the line lie
     * within {@code maxGapWidths} widths, until none is left.
     */
    public static Meander cutOffs(Line line, WidthFn width, double minSepWidths, double maxGapWidths,
                                  double omega, double lambda, int retries) {
        List<Line> loops = new ArrayList<>();
        Line cur = line;
        for (int guard = 0; guard < 200; guard++) {
            double[] w = new double[cur.size()];
            for (int k = 0; k < w.length; k++) w[k] = width.width(cur.x()[k], cur.z()[k]);
            int[] neck = findNeck(cur, w, minSepWidths, maxGapWidths);
            if (neck == null) break;
            int i = neck[0], j = neck[1];
            double[] lx = new double[j - i + 1], lz = new double[j - i + 1];
            System.arraycopy(cur.x(), i, lx, 0, lx.length);
            System.arraycopy(cur.z(), i, lz, 0, lz.length);
            loops.add(new Line(lx, lz));
            List<double[]> pts = new ArrayList<>();
            for (int k = 0; k <= i; k++) pts.add(new double[] {cur.x()[k], cur.z()[k]});
            for (int k = j; k < cur.size(); k++) pts.add(new double[] {cur.x()[k], cur.z()[k]});
            cur = resample(toLine(pts), DS);
        }
        return new Meander(cur, List.copyOf(loops), omega, lambda, retries);
    }

    /** The first (i, j) with arclength apart over minSep widths and distance under maxGap widths, or null. */
    static int[] findNeck(Line l, double[] w, double minSepWidths, double maxGapWidths) {
        int n = l.size();
        double maxW = 0;
        for (double v : w) maxW = Math.max(maxW, v);
        double bucket = Math.max(4.0, maxGapWidths * maxW);
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            minX = Math.min(minX, l.x()[k]); maxX = Math.max(maxX, l.x()[k]);
            minZ = Math.min(minZ, l.z()[k]); maxZ = Math.max(maxZ, l.z()[k]);
        }
        int bw = (int) ((maxX - minX) / bucket) + 1, bh = (int) ((maxZ - minZ) / bucket) + 1;
        int[] head = new int[bw * bh];
        java.util.Arrays.fill(head, -1);
        int[] next = new int[n];
        for (int k = n - 1; k >= 0; k--) {
            int b = (int) ((l.z()[k] - minZ) / bucket) * bw + (int) ((l.x()[k] - minX) / bucket);
            next[k] = head[b];
            head[b] = k;
        }
        for (int i = 0; i < n; i++) {
            double wi = w[Math.min(i, w.length - 1)];
            double gap = maxGapWidths * wi, gap2 = gap * gap;
            int sep = (int) Math.ceil(minSepWidths * wi / DS);
            int bx = (int) ((l.x()[i] - minX) / bucket), bz = (int) ((l.z()[i] - minZ) / bucket);
            int bestJ = -1;
            double bestD = Double.MAX_VALUE;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                int x = bx + dx, z = bz + dz;
                if (x < 0 || z < 0 || x >= bw || z >= bh) continue;
                for (int j = head[z * bw + x]; j >= 0; j = next[j]) {
                    if (j < i + sep) continue;
                    double ex = l.x()[j] - l.x()[i], ez = l.z()[j] - l.z()[i];
                    double d = ex * ex + ez * ez;
                    if (d < gap2 && d < bestD) { bestD = d; bestJ = j; }
                }
            }
            if (bestJ >= 0) return new int[] {i, bestJ};
        }
        return null;
    }

    /**
     * Braid offsets for channel {@code k} of {@code count}: lateral metres off the main line at each point.
     * Pinch nodes every {@code nodeSpacing} metres bring all channels back together.
     */
    public static double[] braidOffsets(int k, int count, int pointCount, double beltHalf, double nodeSpacing,
                                        double nodePhase, int wanderSeed, double[] pin) {
        double lane = count == 1 ? 0 : -0.9 + 1.8 * k / (count - 1);
        double[] off = new double[pointCount];
        for (int i = 0; i < pointCount; i++) {
            double s = i * DS;
            double c = StrictMath.cos(2 * StrictMath.PI * s / nodeSpacing + nodePhase);
            double pinch = 0.06 + 0.94 * Math.sqrt(Noise.clamp01(0.5 - 0.5 * c));
            double wander = 0.3 * Noise.noise1(wanderSeed, s / 170.0);
            off[i] = beltHalf * pinch * Noise.clamp(lane + wander, -1, 1) * pin[i];
        }
        return off;
    }

    static Line toLine(List<double[]> pts) {
        double[] x = new double[pts.size()], z = new double[pts.size()];
        for (int k = 0; k < x.length; k++) { x[k] = pts.get(k)[0]; z[k] = pts.get(k)[1]; }
        return new Line(x, z);
    }

    /** Resamples a polyline to equal steps of {@code ds} metres along its length (keeps both ends). */
    public static Line resample(Line l, double ds) {
        int n = l.size();
        double[] cum = new double[n];
        for (int k = 1; k < n; k++) {
            double dx = l.x()[k] - l.x()[k - 1], dz = l.z()[k] - l.z()[k - 1];
            cum[k] = cum[k - 1] + Math.sqrt(dx * dx + dz * dz);
        }
        double total = cum[n - 1];
        int m = Math.max(2, (int) Math.round(total / ds) + 1);
        double step = total / (m - 1);
        double[] x = new double[m], z = new double[m];
        int seg = 0;
        for (int k = 0; k < m; k++) {
            double s = k == m - 1 ? total : k * step;
            while (seg < n - 2 && cum[seg + 1] < s) seg++;
            double len = cum[seg + 1] - cum[seg];
            double t = len > 0 ? (s - cum[seg]) / len : 0;
            t = Noise.clamp01(t);
            x[k] = l.x()[seg] + (l.x()[seg + 1] - l.x()[seg]) * t;
            z[k] = l.z()[seg] + (l.z()[seg + 1] - l.z()[seg]) * t;
        }
        return new Line(x, z);
    }

    /** Length of a polyline in metres. */
    public static double length(Line l) {
        double s = 0;
        for (int k = 1; k < l.size(); k++) {
            double dx = l.x()[k] - l.x()[k - 1], dz = l.z()[k] - l.z()[k - 1];
            s += Math.sqrt(dx * dx + dz * dz);
        }
        return s;
    }
}
