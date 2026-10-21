package com.yearofice.sim.world;

/**
 * The snow pack of one snow class (Snow cover and permafrost blocks): water equivalent, density, crust and wetness
 * from precipitation, wind and temperature. Depth is derived. Pure Java.
 */
public final class SnowPack {
    private SnowPack() {}

    /** The four snow classes, fixed per column at generation (Blueprint › Snow by class). */
    public enum SnowClass { OPEN, UNDER_SPRUCE, SOUTH_SLOPE, LEE_DRIFT }

    public static final class State {
        public double sweMm, densityKgM3 = 90;
        public boolean crust, wet;

        public double depthCm() { return sweMm <= 0 ? 0 : sweMm / densityKgM3 * 100; }

        public State copy() {
            State s = new State();
            s.sweMm = sweMm; s.densityKgM3 = densityKgM3; s.crust = crust; s.wet = wet;
            return s;
        }
    }

    public static void step(State s, SnowClass cls, SnowTuning t, double meanC, double precipMm, double windMs, int doy) {
        double snowMm = meanC < t.snowBelowC() ? precipMm : 0;
        double rainMm = precipMm - snowMm;
        double fall = snowMm * (cls == SnowClass.UNDER_SPRUCE ? t.underSpruceX() : 1);
        if (cls == SnowClass.LEE_DRIFT && windMs >= t.leeWindMs()) fall += snowMm * (t.leeX() - 1);
        double fallCm = fall * 100 / t.freshDensity();
        if (fall > 0) {
            s.densityKgM3 = (s.sweMm * s.densityKgM3 + fall * t.freshDensity()) / (s.sweMm + fall);
            s.sweMm += fall;
            if (fallCm >= t.crustBuryCm()) s.crust = false;
        }
        s.densityKgM3 += (t.maxDensity() - s.densityKgM3) * t.settlePerDay();
        if (windMs >= t.windPackMs()) s.densityKgM3 = Math.min(t.maxDensity(), s.densityKgM3 + t.windPackDensity());
        boolean wasWet = s.wet;
        if (meanC > 0 && s.sweMm > 0) {
            double x = cls == SnowClass.SOUTH_SLOPE && doy >= t.southFromDoy() ? t.southMeltX() : 1.0;
            s.sweMm = Math.max(0, s.sweMm - t.meltMmPerDegDay() * meanC * x);
        }
        s.wet = s.sweMm > 0 && (meanC > t.wetAboveC() || rainMm > 0);
        if ((wasWet || rainMm > 0) && meanC < t.crustBelowC() && s.sweMm > 0) s.crust = true;
        if (s.sweMm <= 0) {
            s.sweMm = 0; s.densityKgM3 = t.freshDensity(); s.crust = false; s.wet = false;
        }
    }

    /** Whole layers for a depth: rounded, capped at {@code maxLayers}. */
    public static int layers(double depthCm, SnowTuning t) {
        return (int) Math.min(t.maxLayers(), Math.max(0, Math.round(depthCm / t.layerCm())));
    }
}
