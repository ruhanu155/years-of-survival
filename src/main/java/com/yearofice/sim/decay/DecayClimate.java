package com.yearofice.sim.decay;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

/**
 * Climate prefix sums (Decay and time › Climate prefix sums; Minecraft development › Layer 5).
 *
 * <p>Every decay clock is a temperature history with running totals. This class holds those totals as
 * prefix sums over hours, one channel per quantity (decay-hours of a curve, wet hours, rain millimetres,
 * freeze-thaw cycles, melt degree-days, snow-covered hours), so any interval costs two lookups:
 * the total at the end minus the total at the start. That is what makes a chunk unloaded for 3 years and
 * one watched every minute end in the same state.
 *
 * <p>Within an hour the integral is linear, because each hour's value is constant. Channels are kept in
 * insertion order ({@link LinkedHashMap}) so nothing depends on hash order.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class DecayClimate {
    /** The living-rot channel every food clock reads. One unit is one hour at 5 °C. */
    public static final String DECAY_HOURS = "decay_hours";

    private final int hours;
    private final Map<String, Channel> channels;

    private DecayClimate(int hours, Map<String, Channel> channels) {
        this.hours = hours;
        this.channels = Collections.unmodifiableMap(channels);
    }

    public int hours() { return hours; }

    public Set<String> channelNames() { return channels.keySet(); }

    public Channel channel(String name) {
        Channel c = channels.get(name);
        if (c == null) throw new IllegalArgumentException("no climate channel " + name);
        return c;
    }

    /** Cumulative decay-hours (living rot) at an hour mark. */
    public double decayHoursAt(double hour) { return channel(DECAY_HOURS).at(hour); }

    /** Decay-days between two hour marks: one day at 5 °C, damp, is one decay-day. */
    public double decayDaysBetween(double fromHour, double toHour) {
        return channel(DECAY_HOURS).between(fromHour, toHour) / 24.0;
    }

    public static Builder builder(double[] hourlyTempC) { return new Builder(hourlyTempC); }

    /**
     * A climate with only the living-rot channel from a temperature record and a cold curve.
     */
    public static DecayClimate livingRot(double[] hourlyTempC, ColdCurve curve) {
        return builder(hourlyTempC).rated(DECAY_HOURS, curve.livingRot()).build();
    }

    /**
     * The first day at or after {@code fromDay} whose trailing 7-day mean air passes the frozen line:
     * the store's normal thaw date (Decay and time › The cold curve). Days index {@code dailyMeanC};
     * the window wraps around the year. Returns −1 if the mean never passes it (the permafrost cellar).
     */
    public static int normalThawDay(double[] dailyMeanC, int fromDay, double frozenBelowC) {
        int n = dailyMeanC.length;
        if (n < 7) throw new IllegalArgumentException("need at least 7 days");
        for (int step = 0; step < n; step++) {
            int d = Math.floorMod(fromDay + step, n);
            double sum = 0;
            for (int i = 0; i < 7; i++) sum += dailyMeanC[Math.floorMod(d - i, n)];
            if (sum / 7.0 > frozenBelowC) return d;
        }
        return -1;
    }

    /** One prefix-summed quantity. {@code prefix[h]} is the total over hours [0, h). */
    public static final class Channel {
        private final double[] prefix;

        private Channel(double[] perHour) {
            prefix = new double[perHour.length + 1];
            double run = 0;
            for (int h = 0; h < perHour.length; h++) {
                run += perHour[h];
                prefix[h + 1] = run;
            }
        }

        public static Channel of(double[] perHour) { return new Channel(perHour.clone()); }

        public int hours() { return prefix.length - 1; }

        /** The running total at an hour mark, linear inside the hour; clamped to the record's ends. */
        public double at(double hour) {
            if (hour <= 0) return 0.0;
            int n = prefix.length - 1;
            if (hour >= n) return prefix[n];
            int h = (int) hour;
            double frac = hour - h;
            return frac == 0.0 ? prefix[h] : prefix[h] + (prefix[h + 1] - prefix[h]) * frac;
        }

        public double between(double fromHour, double toHour) { return at(toHour) - at(fromHour); }

        /** The rate (value per hour) during hour h. */
        public double rateAt(int hour) { return prefix[hour + 1] - prefix[hour]; }
    }

    public static final class Builder {
        private final double[] temps;
        private final LinkedHashMap<String, Channel> channels = new LinkedHashMap<>();

        private Builder(double[] hourlyTempC) { this.temps = hourlyTempC.clone(); }

        /** A channel whose hourly value is a rate function of the hour's temperature. */
        public Builder rated(String name, DoubleUnaryOperator rate) {
            double[] v = new double[temps.length];
            for (int h = 0; h < temps.length; h++) v[h] = rate.applyAsDouble(temps[h]);
            channels.put(name, new Channel(v));
            return this;
        }

        /** A channel from precomputed hourly values (rain mm, wet flags, melt degree-hours). */
        public Builder values(String name, double[] perHour) {
            if (perHour.length != temps.length) throw new IllegalArgumentException("length mismatch for " + name);
            channels.put(name, Channel.of(perHour));
            return this;
        }

        /** Freeze-thaw crossings: +1 in each hour where the temperature crosses 0 °C from the hour before. */
        public Builder freezeThaw(String name) {
            double[] v = new double[temps.length];
            for (int h = 1; h < temps.length; h++) {
                if ((temps[h - 1] < 0) != (temps[h] < 0)) v[h] = 0.5; // two crossings make one cycle
            }
            channels.put(name, new Channel(v));
            return this;
        }

        public DecayClimate build() { return new DecayClimate(temps.length, new LinkedHashMap<>(channels)); }
    }
}
