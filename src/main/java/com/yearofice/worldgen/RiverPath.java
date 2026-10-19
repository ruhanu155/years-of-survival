package com.yearofice.worldgen;

/**
 * The main river's path, laid by stage 2 (Terrain and hydrology › The main river's path): inside the middle
 * third, from the entry edge to the exit edge, offset sideways by two octaves of 1D noise (1/400 m and
 * 1/150 m) scaled by the archetype's swing. The path is the smooth curve {@code z = P(x)}; the polyline the
 * later stages read is that curve sampled every 16 m. Beyond the core edges it runs straight on to the world
 * border, so the coarse field keeps the valley.
 *
 * <p>Distance to the path is the first-order distance to the curve, {@code |z − P(x)| / sqrt(1 + P'(x)^2)},
 * with P and P' smoothed over a window that grows with distance: exact for straight reaches, continuous and
 * smooth everywhere, so the trough has no medial-axis creases at bends and far hills no stripes.
 *
 * <p>Coordinates are the canonical frame: metres from the core's north-west corner, river flowing west to
 * east (+x). Mirroring is applied by the caller.
 */
public final class RiverPath {
    private final double length, entryZ, exitZ, swing, longScale, shortScale, longWeight, bandLo, bandHi;
    private final int longSeed, shortSeed;
    private final double[] px, pz;

    public RiverPath(double length, double entryZ, double exitZ, double swing, int longSeed, int shortSeed,
                     WorldgenParams.RiverPathParams rp) {
        this.length = length;
        this.entryZ = entryZ;
        this.exitZ = exitZ;
        this.swing = swing;
        this.longSeed = longSeed;
        this.shortSeed = shortSeed;
        this.longScale = rp.longScaleM();
        this.shortScale = rp.shortScaleM();
        this.longWeight = rp.longWeight();
        this.bandLo = rp.bandMinFraction() * length;
        this.bandHi = rp.bandMaxFraction() * length;
        int points = (int) (length / rp.pointSpacingM()) + 1;
        px = new double[points];
        pz = new double[points];
        for (int i = 0; i < points; i++) {
            px[i] = i * rp.pointSpacingM();
            pz[i] = zAt(px[i]);
        }
    }

    public int corePointCount() { return px.length; }

    public double coreX(int i) { return px[i]; }

    public double coreZ(int i) { return pz[i]; }

    public double coreLength() { return length; }

    /** z of the path at canonical x; constant beyond the core edges. */
    public double zAt(double x) {
        if (x < 0) x = 0;
        if (x > length) x = length;
        double line = entryZ + (exitZ - entryZ) * (x / length);
        double off = longWeight * Noise.noise1(longSeed, x / longScale) + (1 - longWeight) * Noise.noise1(shortSeed, x / shortScale);
        // Pin the ends to the drawn edge offsets so the river meets its border crossing.
        double pin = Noise.smooth(x / 160.0) * Noise.smooth((length - x) / 160.0);
        return softBand(line + swing * off * pin);
    }

    /** Keeps the path inside the middle band with a C1 soft limit over the last 60 m. */
    private double softBand(double v) {
        double m = 60;
        if (v > bandHi - m) {
            double t = (v - (bandHi - m)) / m;
            return bandHi - m + m * t / (1 + t);
        }
        if (v < bandLo + m) {
            double t = ((bandLo + m) - v) / m;
            return bandLo + m - m * t / (1 + t);
        }
        return v;
    }

    /** Smooth distance from (x, z) to the path, and which side it lies on. */
    public double distance(double x, double z) {
        // Far points see the path smoothed over a window that grows with their distance (the nearest path
        // point of a point 500 m away lies within a few hundred metres of x), so the path's 150 m wiggles do
        // not print as north–south stripes across the hills. Near the river the window is 1 m: exact.
        double near = z - zAt(x);
        if (near < 0) near = -near;
        double h = Noise.clamp(near * 0.6, 1.0, 600.0);
        double p = (zAt(x - h) + 2 * zAt(x - h / 2) + 3 * zAt(x) + 2 * zAt(x + h / 2) + zAt(x + h)) / 9.0;
        double slope = (zAt(x + h) - zAt(x - h)) / (2 * h);
        double dz = z - p;
        if (dz < 0) dz = -dz;
        return dz / Math.sqrt(1.0 + slope * slope);
    }

    /** True when (x, z) lies north of the path (smaller z). */
    public boolean north(double x, double z) { return z < zAt(x); }
}
