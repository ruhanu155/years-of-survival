/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022-present Wendall Cada (wendall911)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Adapted for Year of Ice from wendall911/Homeostatic@934c96cc0af2631c7d9d0122e621fceebd420b64,
 * Common/src/main/java/homeostatic/common/wetness/WetnessInfo.java, util/WetnessHelper.java and
 * common/temperature/EnvironmentData.java (moisture sources) / BodyTemperature.java (drying).
 */
package com.yearofice.sim.body;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;

/**
 * Wetness gain and drying, per slot, each game minute.
 *
 * <p>From Homeostatic: the source ladder (submerged soaks at once, partial submersion soaks what it
 * reaches, rain adds a little each update), waterproof gear cutting what gets through, and drying only
 * while nothing is wetting. Ours on top: wetness per zone and layer instead of one body number, the
 * outer layer taking rain first and passing it to the inner as it soaks, and the Blueprint's drying
 * rates (Clothing › Wet and drying; Waters › Drying off).
 */
public final class WetnessModel {
    private WetnessModel() {}

    /**
     * Rain wetting, per mm/h, per game minute, on an unprotected layer. The Blueprint gives no rate; this
     * anchors on its one figure, wet snow soaking boots in 30 game minutes, read as moderate 2.5 mm/h:
     * 1/30 per minute ÷ 2.5. A {@code [body]} tuning key in the adapter round.
     */
    public static final double RAIN_PER_MMH_MIN = 1.0 / 30.0 / 2.5;
    /** Snow wets only as it melts on the body: half the rain rate when air is above 0 °C, none below. */
    public static final double SNOW_MELT_FACTOR = 0.5;
    /** Sweat: inner layers +10% a minute. */
    public static final double SWEAT_PER_MIN = 0.10;
    public static final double SWEAT_CLOTHING_C = 20, SWEAT_ABOVE_C = -10, HEAT_SPELL_C = 28;
    /** Fever's hot phase wets the inner torso layer 10% an hour under an outer layer. */
    public static final double FEVER_PER_MIN = 0.10 / 60;
    /** Drying on the body, per game hour: 10% above 10 °C, 5% at 0–10 °C, none below 0 °C (while moving). */
    public static final double BODY_DRY_WARM = 0.10, BODY_DRY_COOL = 0.05;
    /** Resting dries at this share of the moving rate (the Blueprint names only "while moving"). */
    public static final double RESTING_DRY_SHARE = 0.5;
    /** By the fire about 1 game hour, on the line about 4. */
    public static final double FIRE_DRY_PER_H = 1.0, LINE_DRY_PER_H = 0.25;

    public enum Drying { BODY, FIRE }

    /** The minute's exposure, gathered by the adapter. */
    public record Exposure(Precip precip, double precipMmH, double airC, double rainFactor, ColdWater.Depth depth,
                           Activity activity, boolean moving, boolean feverHot, Drying drying) {

        public static Exposure dry(double airC) {
            return new Exposure(Precip.NONE, 0, airC, 1, ColdWater.Depth.NONE, Activity.REST, false, false, Drying.BODY);
        }

        /** Exposure as an outdoor sample reads it, with the open shelter's rain factor. */
        public static Exposure of(EnvSample env, double rainFactor, Activity a, boolean moving, boolean nearFire) {
            double rf = env.indoors() ? 0 : rainFactor;
            return new Exposure(env.precip(), env.precipMmH(), env.airC(), rf, ColdWater.Depth.NONE, a, moving, false,
                    nearFire ? Drying.FIRE : Drying.BODY);
        }

        public Exposure in(ColdWater.Depth d) {
            return new Exposure(precip, precipMmH, airC, rainFactor, d, activity, moving, feverHot, drying);
        }

        public Exposure doing(Activity a, boolean isMoving) {
            return new Exposure(precip, precipMmH, airC, rainFactor, depth, a, isMoving, feverHot, drying);
        }

        public Exposure drying(Drying d) {
            return new Exposure(precip, precipMmH, airC, rainFactor, depth, activity, moving, feverHot, d);
        }
    }

    /** Rain reaching the body this minute, as a wetting rate per minute on an unprotected layer. */
    public static double rainRate(Exposure e) {
        if (e.precipMmH() <= 0 || e.rainFactor() <= 0) return 0;
        double r;
        if (e.precip().liquid()) r = RAIN_PER_MMH_MIN * e.precipMmH();
        else if (e.precip() == Precip.SNOW) r = e.airC() > 0 ? RAIN_PER_MMH_MIN * SNOW_MELT_FACTOR * e.precipMmH() : 0;
        else r = 0;
        return r * e.rainFactor();
    }

    /** One game minute of wetting and drying. */
    public static ClothingState stepMinute(ClothingState s, Exposure e) { return stepMinute(s, e, 1.0); }

    /**
     * As above, with {@code handsMult} on what the hand slots gain from rain and sweat (greased gloves, B13). A
     * dunk in water soaks them whatever the grease.
     */
    public static ClothingState stepMinute(ClothingState s, Exposure e, double handsMult) {
        if (handsMult == 1.0 || e.depth() != ColdWater.Depth.NONE) return stepMinute0(s, e);
        ClothingState c = stepMinute0(s, e);
        double[] w = c.wetCopy();
        for (Layer l : Layer.values()) {
            int i = Layer.slot(Zone.HANDS, l);
            double gain = w[i] - s.wet(i);
            if (gain > 0) w[i] = s.wet(i) + gain * handsMult;
        }
        return c.withWet(w);
    }

    private static ClothingState stepMinute0(ClothingState s, Exposure e) {
        double[] w = s.wetCopy();
        boolean[] gained = new boolean[Layer.SLOTS];

        // 1. Immersion: Homeostatic's "submerged → full moisture", per zone the water reaches.
        immerse(s, e.depth(), w, gained);

        // 2. Rain: outer layer first; it passes on to the inner as it soaks; waterproof outers shed it all.
        double r = rainRate(e);
        if (r > 0) {
            for (Zone z : Zone.values()) {
                int in = Layer.slot(z, Layer.INNER), out = Layer.slot(z, Layer.OUTER);
                WornGarment outer = s.at(out);
                if (outer == null) {
                    add(w, gained, in, r);
                } else if (!outer.sheds()) {
                    double through = r * w[out];
                    add(w, gained, out, r);
                    add(w, gained, in, through);
                }
            }
        }

        // 3. Sweat under heavy clothing, unless the coat is open.
        if (e.activity().sweaty() && !s.coatOpen() && e.airC() > SWEAT_ABOVE_C
                && Clothing.warmthC(s) > SWEAT_CLOTHING_C) {
            double rate = SWEAT_PER_MIN * (e.activity() == Activity.HEAVY && e.airC() > HEAT_SPELL_C ? 2 : 1);
            for (Zone z : Zone.values()) add(w, gained, Layer.slot(z, Layer.INNER), rate);
        }
        if (e.feverHot() && s.at(Zone.TORSO, Layer.OUTER) != null) {
            add(w, gained, Layer.slot(Zone.TORSO, Layer.INNER), FEVER_PER_MIN);
        }

        // 4. Drying, only where nothing wetted this minute; an inner layer under a wet outer waits.
        double dry = dryPerMinute(e);
        if (dry > 0) {
            for (Zone z : Zone.values()) {
                int in = Layer.slot(z, Layer.INNER), out = Layer.slot(z, Layer.OUTER);
                if (!gained[out]) w[out] = Math.max(0, w[out] - dry);
                boolean outerWet = s.at(out) != null && w[out] > Clothing.WET_MARK;
                if (!gained[in] && !outerWet) w[in] = Math.max(0, w[in] - dry);
            }
        }
        return s.withWet(w);
    }

    /** {@code minutes} steps at constant exposure. */
    public static ClothingState step(ClothingState s, Exposure e, int minutes) {
        ClothingState c = s;
        for (int i = 0; i < minutes; i++) c = stepMinute(c, e);
        return c;
    }

    /** Drying per minute for worn clothing in this exposure. */
    public static double dryPerMinute(Exposure e) {
        if (e.depth() != ColdWater.Depth.NONE) return 0;
        if (e.drying() == Drying.FIRE) return FIRE_DRY_PER_H / 60;
        double perH;
        if (e.airC() > 10) perH = BODY_DRY_WARM;
        else if (e.airC() >= 0) perH = BODY_DRY_COOL;
        else perH = 0;
        if (!e.moving()) perH *= RESTING_DRY_SHARE;
        return perH / 60;
    }

    /** Drying for a loose item hung on the line or by the fire, {@code minutes} game minutes. */
    public static double dryLoose(double wet, Drying where, boolean onLine, int minutes) {
        double perH = where == Drying.FIRE ? FIRE_DRY_PER_H : (onLine ? LINE_DRY_PER_H : 0);
        return Math.max(0, wet - perH * minutes / 60.0);
    }

    /** Wick a share of wetness off every slot (rolling in dry powder snow below −15 °C takes 20%). */
    public static ClothingState wick(ClothingState s, double share) {
        double[] w = s.wetCopy();
        for (int i = 0; i < w.length; i++) w[i] *= (1 - share);
        return s.withWet(w);
    }

    private static void immerse(ClothingState s, ColdWater.Depth d, double[] w, boolean[] gained) {
        switch (d) {
            case SWIM -> {
                for (int i = 0; i < Layer.SLOTS; i++) soak(w, gained, i, 1);
            }
            case THIGH -> {
                soakZone(w, gained, Zone.FEET, 1);
                soakZone(w, gained, Zone.LEGS, 1);
            }
            case KNEE -> {
                soakZone(w, gained, Zone.FEET, 1);
                soakZone(w, gained, Zone.LEGS, 0.5);
            }
            case ANKLE -> {
                WornGarment boots = s.at(Zone.FEET, Layer.OUTER);
                if (boots == null || !boots.sheds()) soakZone(w, gained, Zone.FEET, 1);
            }
            default -> { }
        }
    }

    private static void soakZone(double[] w, boolean[] gained, Zone z, double level) {
        soak(w, gained, Layer.slot(z, Layer.INNER), level);
        soak(w, gained, Layer.slot(z, Layer.OUTER), level);
    }

    private static void soak(double[] w, boolean[] gained, int slot, double level) {
        if (w[slot] < level) w[slot] = level;
        gained[slot] = true;
    }

    private static void add(double[] w, boolean[] gained, int slot, double amount) {
        if (amount <= 0) return;
        w[slot] = Math.min(1, w[slot] + amount);
        gained[slot] = true;
    }
}
