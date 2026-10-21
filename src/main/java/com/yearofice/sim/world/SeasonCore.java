package com.yearofice.sim.world;

import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SnowPack.SnowClass;

/**
 * The whole year's state of one level, advanced a day at a time: four snow packs, four ice records, the flood
 * pulse. {@link #step} is a pure function of the previous state and the day's weather, so a world that was away
 * replays the missed days and ends where a live one would (Catch-up on chunk load).
 */
public final class SeasonCore {
    public final SnowPack.State[] snow = new SnowPack.State[SnowClass.values().length];
    public final IceModel.State[] ice = new IceModel.State[BodyClass.values().length];
    public double floodM;
    public long lastDay = Long.MIN_VALUE;
    public final IceModel.Event[] lastEvent = new IceModel.Event[BodyClass.values().length];

    public SeasonCore() {
        for (int i = 0; i < snow.length; i++) snow[i] = new SnowPack.State();
        for (int i = 0; i < ice.length; i++) { ice[i] = new IceModel.State(); lastEvent[i] = IceModel.Event.NONE; }
    }

    public SnowPack.State snow(SnowClass c) { return snow[c.ordinal()]; }

    public IceModel.State ice(BodyClass c) { return ice[c.ordinal()]; }

    /** Advances to {@code absDay}'s weather (one call per day, in order). */
    public void step(long absDay, int doy, double meanC, double precipMm, double windMs, SnowTuning st, IceTuning it) {
        for (SnowClass c : SnowClass.values()) SnowPack.step(snow(c), c, st, meanC, precipMm, windMs, doy);
        double rain = meanC < st.snowBelowC() ? 0 : precipMm;
        double openSnow = snow(SnowClass.OPEN).depthCm();
        for (BodyClass c : BodyClass.values()) {
            IceModel.Event e = IceModel.step(ice(c), c, it, meanC, rain, openSnow);
            lastEvent[c.ordinal()] = e;
            if (e == IceModel.Event.BREAKUP) floodM = Math.max(floodM, it.floodPulseM() + it.floodPerSnowCm() * openSnow);
        }
        if (floodM > 0) {
            floodM *= it.floodKeepPerDay();
            if (floodM < it.floodMinM()) floodM = 0;
        }
        lastDay = absDay;
    }

    /** Blocks of flood water for a level: rounded, 0 when under half a block. */
    public static int floodBlocks(double floodM) { return (int) Math.floor(floodM + 0.5); }

    /** Flat encoding for SavedData: snow (4 x 4), ice (4 x 4), flood, lastDay. */
    public double[] encode() {
        double[] a = new double[snow.length * 4 + ice.length * 4 + 1];
        int i = 0;
        for (SnowPack.State s : snow) { a[i++] = s.sweMm; a[i++] = s.densityKgM3; a[i++] = s.crust ? 1 : 0; a[i++] = s.wet ? 1 : 0; }
        for (IceModel.State s : ice) { a[i++] = s.cm; a[i++] = s.fdd; a[i++] = s.peakCm; a[i++] = s.rotten ? 1 : 0; }
        a[i] = floodM;
        return a;
    }

    public void decode(double[] a) {
        int i = 0;
        for (SnowPack.State s : snow) { s.sweMm = a[i++]; s.densityKgM3 = a[i++]; s.crust = a[i++] > 0; s.wet = a[i++] > 0; }
        for (IceModel.State s : ice) { s.cm = a[i++]; s.fdd = a[i++]; s.peakCm = a[i++]; s.rotten = a[i++] > 0; }
        floodM = a[i];
    }
}
