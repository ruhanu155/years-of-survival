package com.yearofice.sim.decay;

import java.util.function.DoubleUnaryOperator;

/**
 * The temperature factor f_T for every decay curve (Decay and time › The cold curve).
 *
 * <p>Living rot follows the Spoilage model written as a rate: each 10 °C colder divides the rate by 3,
 * 0.2 while half-frozen (−5 ≤ T < 0 °C) and 0 once frozen below −5 °C. The rate is read from a table of
 * whole degrees (Blueprint › Spoilage model: "the code reads L from a table of whole degrees, not from
 * pow()"), built once with {@link StrictMath} and interpolated linearly between whole degrees above 0 °C,
 * so every JVM and both sides of the network get the same bits.
 *
 * <p>Fixed-life curves (smoked, dried, pemmican, lard) count their own life at rate 1 from −5 to 15 °C,
 * faster above 15 °C by the ratio of their cool and warm lives, and stop below −5 °C.
 *
 * <p>Pure Java, no Minecraft imports. Original Year of Ice code.
 */
public final class ColdCurve {
    /** The default frozen threshold, {@code [decay] frozen_below_c} in yearofice-common.toml. */
    public static final double DEFAULT_FROZEN_BELOW_C = -5.0;
    /** The half-frozen rate between the frozen threshold and 0 °C. */
    public static final double HALF_FROZEN_RATE = 0.2;
    /** Reference temperature: one decay-hour is one hour at 5 °C. */
    public static final double REFERENCE_C = 5.0;
    /** Fixed-life curves run warm above this. */
    public static final double FIXED_LIFE_WARM_ABOVE_C = 15.0;
    /** Flies add ×1.5 to uncovered fresh meat or fish above this (Spoilage model, place factor table). */
    public static final double FLIES_ABOVE_C = 10.0;
    public static final double FLIES_FACTOR = 1.5;

    private static final int TABLE_MIN_C = 0;
    private static final int TABLE_MAX_C = 60;

    /** The Standard-preset curve with the frozen line at −5 °C. */
    public static final ColdCurve STANDARD = new ColdCurve(DEFAULT_FROZEN_BELOW_C);

    private final double frozenBelowC;
    private final double[] table;

    public ColdCurve(double frozenBelowC) {
        if (frozenBelowC > 0) throw new IllegalArgumentException("frozen threshold must be at or below 0 °C");
        this.frozenBelowC = frozenBelowC;
        this.table = new double[TABLE_MAX_C - TABLE_MIN_C + 1];
        for (int t = TABLE_MIN_C; t <= TABLE_MAX_C; t++) {
            table[t - TABLE_MIN_C] = StrictMath.pow(3.0, (t - REFERENCE_C) / 10.0);
        }
    }

    public double frozenBelowC() { return frozenBelowC; }

    /** f_T for living rot: the rate relative to 5 °C. 1.0 at 5 °C, 3.0 at 15 °C, 0.2 half-frozen, 0 frozen. */
    public double rate(double tempC) {
        if (Double.isNaN(tempC)) throw new IllegalArgumentException("temperature is NaN");
        if (tempC < frozenBelowC) return 0.0;
        if (tempC < 0.0) return HALF_FROZEN_RATE;
        if (tempC >= TABLE_MAX_C) return table[table.length - 1];
        int lo = (int) tempC; // tempC >= 0, so this is floor
        double frac = tempC - lo;
        double a = table[lo - TABLE_MIN_C];
        if (frac == 0.0) return a;
        double b = table[lo + 1 - TABLE_MIN_C];
        return a + (b - a) * frac;
    }

    /** True when food at this temperature is frozen and its living-rot clock stops. */
    public boolean frozen(double tempC) { return tempC < frozenBelowC; }

    /**
     * Safe life L in days at a temperature for a food with factor k: the time from freshness 100 to 30.
     * L(T) = 6 · k / f_T(T). Infinite when frozen.
     */
    public double safeLifeDays(double tempC, double k) {
        double r = rate(tempC);
        return r == 0.0 ? Double.POSITIVE_INFINITY : 6.0 * k / r;
    }

    /** Living rot with the flies factor folded in, for uncovered fresh meat or fish on the ground. */
    public DoubleUnaryOperator withFlies() {
        return t -> rate(t) * (t > FLIES_ABOVE_C ? FLIES_FACTOR : 1.0);
    }

    /** Living rot as a rate function, for building prefix sums. */
    public DoubleUnaryOperator livingRot() { return this::rate; }

    /**
     * Rate of a fixed-life curve: 1 between the frozen line and 15 °C, coolLife ÷ warmLife above 15 °C, 0 frozen.
     * Hot-smoked is 30 and 14 days, so it runs ×2.14 above 15 °C.
     */
    public double fixedLifeRate(double tempC, double coolLifeDays, double warmLifeDays) {
        if (tempC < frozenBelowC) return 0.0;
        if (tempC <= FIXED_LIFE_WARM_ABOVE_C) return 1.0;
        return coolLifeDays / warmLifeDays;
    }

    public DoubleUnaryOperator fixedLife(double coolLifeDays, double warmLifeDays) {
        return t -> fixedLifeRate(t, coolLifeDays, warmLifeDays);
    }

    /**
     * Chemical driver for rust: no rust below 0 °C (liquid water needed), reference rate above.
     * Decay and time › Seven factors: "0 below 0 °C for rust".
     */
    public static double rust(double tempC) { return tempC < 0.0 ? 0.0 : 1.0; }

    /**
     * Temperature a store sits at under 30 cm of snow or more: the 7-day mean of the air, capped at 0 °C
     * (Decay and time › Snow as a cold store). Snow pits hold 0 °C while snow remains, which is the same cap.
     */
    public static double underSnow(double sevenDayMeanAirC) { return Math.min(0.0, sevenDayMeanAirC); }

    /** Minimum snow depth that makes a store a snow store. */
    public static final double SNOW_STORE_MIN_CM = 30.0;
}
