package com.yearofice.sim.fire;

import com.yearofice.sim.env.Precip;

/**
 * What the weather does at one fire this minute: precipitation, whether it is heavy, the wind, and whether a roof,
 * lean-to or tarp covers the fire. The env layer will produce this from {@code EnvService.sample}; until it lands,
 * {@code com.yearofice.fire.FireEnv} reads vanilla rain, thunder and sky access.
 */
public record FireWeather(Precip precip, boolean heavy, double windMs, boolean covered) {

    public static final FireWeather CALM = new FireWeather(Precip.NONE, false, 0, false);

    public FireWeather {
        if (precip == null) precip = Precip.NONE;
        if (windMs < 0) windMs = 0;
    }

    /** Liquid rain reaching an uncovered fire. */
    public boolean rainOnFire() { return !covered && precip.liquid(); }

    /** Snow falling on an uncovered fire. */
    public boolean snowOnFire() { return !covered && precip == Precip.SNOW; }

    public FireWeather withWind(double ms) { return new FireWeather(precip, heavy, ms, covered); }

    public static FireWeather heavyRain() { return new FireWeather(Precip.RAIN, true, 0, false); }

    public static FireWeather lightRain() { return new FireWeather(Precip.DRIZZLE, false, 0, false); }

    public static FireWeather snow() { return new FireWeather(Precip.SNOW, false, 0, false); }
}
