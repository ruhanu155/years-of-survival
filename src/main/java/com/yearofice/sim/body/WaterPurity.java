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
 * Common/src/main/java/homeostatic/util/WaterHelper.java (drink: dirty water rolls a chance)
 * and Common/src/main/java/homeostatic/common/Hydration.java (purity per drink source).
 */
package com.yearofice.sim.body;

import com.yearofice.core.hash.Mulberry32;

/**
 * Water purity and boiling (Blueprint › Fire, water and light › Water, Water quality, Boiling).
 *
 * <p>From Homeostatic: each drink source carries a purity, and a drink of impure water rolls one chance
 * for illness. Ours: three purity classes with the Blueprint's multipliers on the preset's base chance
 * (15/30/40%), boiling 5 game minutes at a rolling boil making any water safe, and onset 1–3 game days
 * later. Homeostatic halves the chance while the player is not already ill; we do not, because the
 * Blueprint says bad water again simply restarts the gut-illness clock.
 */
public final class WaterPurity {
    private WaterPurity() {}

    public enum Purity {
        /** Rain, melted snow, birch sap, the hot spring: always safe. */
        SAFE(0),
        /** Moving and open water: the preset's base chance. */
        RAW(1),
        /** Beaver ponds and within 100 m downstream of a latrine, cleaning spot or carcass: double. */
        FOUL(2);

        public final double multiplier;

        Purity(double m) { this.multiplier = m; }
    }

    public enum Source {
        RIVER(Purity.RAW), LAKE(Purity.RAW), CREEK(Purity.RAW),
        BEAVER_POND(Purity.FOUL), DOWNSTREAM_OF_WASTE(Purity.FOUL),
        RAIN(Purity.SAFE), MELTED_SNOW(Purity.SAFE), MELTED_ICE(Purity.SAFE), BIRCH_SAP(Purity.SAFE),
        HOT_SPRING(Purity.SAFE);

        public final Purity purity;

        Source(Purity p) { this.purity = p; }
    }

    /** Minutes at a rolling boil that make water safe. */
    public static final double SAFE_BOIL_MIN = 5;
    /** Illness shows 1–3 game days after the drink. */
    public static final int ONSET_MIN = 1440, ONSET_MAX = 3 * 1440;

    /** The chance one drink makes the player ill. */
    public static double illnessChance(Purity p, boolean boiled, double presetBase) {
        if (boiled) return 0;
        return Math.max(0, Math.min(1, presetBase * p.multiplier));
    }

    public static double illnessChance(Source s, boolean boiled, BodyPreset preset) {
        return illnessChance(s.purity, boiled, preset.unboiledIllness);
    }

    /** One drink's outcome; {@code onsetMinutes} is −1 when the drink was fine. */
    public record Drink(boolean ill, int onsetMinutes) {}

    /**
     * Rolls one drink. Always draws two numbers so a stream stays aligned whether or not the drink was bad.
     * Seed {@code rng} from a named stream, for example
     * {@code new Mulberry32(StageHash.of(seed + ":drink:" + player + ":" + minute))}.
     */
    public static Drink drink(Purity p, boolean boiled, double presetBase, Mulberry32 rng) {
        double u = rng.nextDouble();
        double v = rng.nextDouble();
        if (u >= illnessChance(p, boiled, presetBase)) return new Drink(false, -1);
        return new Drink(true, ONSET_MIN + (int) (v * (ONSET_MAX - ONSET_MIN + 1)));
    }

    /** Heat under the pot and its minutes to bring 1 L to a boil. */
    public enum Heat {
        SMALL_FIRE(30), STEADY_FIRE(20), HOT_FIRE(12), TEEPEE(12), COALS(20), STOVE(15);

        public final double minutesPerLitre;

        Heat(double m) { this.minutesPerLitre = m; }
    }

    /**
     * Minutes to a boil. 1 L takes the table's time and a full 2 L pot 1.6 times as long; other volumes run
     * on the line through those two points, never below 0.4 of the 1 L time.
     */
    public static double minutesToBoil(Heat h, double litres) {
        double factor = Math.max(0.4, 1 + 0.6 * (litres - 1));
        return h.minutesPerLitre * factor;
    }

    /** Minutes from cold until the water is safe: to the boil, then 5 at a rolling boil. */
    public static double minutesToSafe(Heat h, double litres) {
        return minutesToBoil(h, litres) + SAFE_BOIL_MIN;
    }

    /** Hot stones in the birch-bark bucket: 4 L boils in 10 game minutes, then 5 more to be safe. */
    public static double hotStonesMinutesToSafe() { return 10 + SAFE_BOIL_MIN; }

    public static boolean safeAfterRollingBoil(double minutesAtBoil) { return minutesAtBoil >= SAFE_BOIL_MIN; }
}
