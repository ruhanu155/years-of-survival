package com.yearofice.sim.decay;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongToDoubleFunction;

/**
 * An {@code air:} decay clock (Minecraft development › Layer 5, A decay clock): D(t) = Σ_h r(T(h)), one decay-hour
 * being one hour at 5 °C. Each game hour runs at the rate of the air at its start, so D is linear inside an hour
 * and the value at any minute costs one lookup once the hours up to it are summed. The hours are summed lazily
 * and kept (the Layer 5 table's "computed from the year table, cumulative ... cached at load"), so a carcass
 * stamped at a past dawn and a stack read now agree exactly. It is the growing, live form of
 * {@link DecayClimate}'s decay-hours channel.
 *
 * <p>Hour 0 is 00:00 on May 1 of year 1. Before it (negative minutes) the clock runs at hour 0's rate.
 * Pure Java; thread-safe.
 */
public final class AirClock {
    private final LongToDoubleFunction hourAirC;
    private final DoubleUnaryOperator rate;
    /** {@code cum[h]} is D at the start of hour h; valid for h ≤ {@code filled}. */
    private double[] cum = new double[256];
    /** {@code rates[h]} is hour h's rate; valid for h < {@code filled}. */
    private double[] rates = new double[256];
    private int filled;

    /**
     * @param hourAirC air in °C at the start of absolute game hour h (h ≥ 0)
     * @param rate     the curve's rate at a temperature, 1.0 at 5 °C
     */
    public AirClock(LongToDoubleFunction hourAirC, DoubleUnaryOperator rate) {
        this.hourAirC = hourAirC;
        this.rate = rate;
    }

    /** Decay-hours at absolute game minute {@code minute}. */
    public synchronized double dAt(double minute) {
        double hours = minute / 60.0;
        if (hours < 0) return hours * rateOfHour(0);
        long h = (long) Math.floor(hours);
        fill(h + 1);
        int i = (int) h;
        return cum[i] + (hours - h) * rates[i];
    }

    /** The rate in force at {@code minute}: its hour's r(T). */
    public synchronized double rateAt(double minute) {
        return rateOfHour(Math.max(0, (long) Math.floor(minute / 60.0)));
    }

    /** The air at the start of {@code minute}'s hour, °C. */
    public double airAt(double minute) { return hourAirC.applyAsDouble(Math.max(0, (long) Math.floor(minute / 60.0))); }

    private double rateOfHour(long h) {
        fill(h + 1);
        return rates[(int) h];
    }

    /** Sums every hour below {@code toHour}. */
    private void fill(long toHour) {
        if (toHour > Integer.MAX_VALUE - 1) throw new IllegalArgumentException("hour " + toHour);
        int to = (int) toHour;
        if (to <= filled) return;
        if (to >= cum.length) {
            int n = Math.max(cum.length * 2, to + 1);
            cum = Arrays.copyOf(cum, n);
            rates = Arrays.copyOf(rates, n);
        }
        for (int h = filled; h < to; h++) {
            double r = Math.max(0, rate.applyAsDouble(hourAirC.applyAsDouble(h)));
            rates[h] = r;
            cum[h + 1] = cum[h] + r;
        }
        filled = to;
    }

    /** Hours summed so far (debug). */
    public synchronized int hoursSummed() { return filled; }
}
