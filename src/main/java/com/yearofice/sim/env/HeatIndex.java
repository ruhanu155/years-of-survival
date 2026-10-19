/*
 * Cold Sweat
 * Copyright (C) Momo-Softworks (MikulDev) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Adapted for Year of Ice from Momo-Softworks/Cold-Sweat@4ebc97e37e95168819f3d17ed40708dc7a6f2fa7,
 * src/main/java/com/momosoftworks/coldsweat/api/temperature/modifier/BlockTempModifier.java
 * and util/math/CSMath.java (blend). Full licence text: licenses/cold-sweat.txt.
 * RELEASE BLOCKER: GPL-3.0-derived file; swap before any public release.
 */
package com.yearofice.sim.env;

import java.util.List;

/**
 * The heat index: what nearby fires, stoves and other sources add to feels-like temperature at a point.
 *
 * <p>Structure taken from Cold Sweat's block-temperature scan: each source contributes its value faded
 * in a straight line with distance ({@code CSMath.blend}), each solid block between source and player
 * divides the contribution by (blocks + 1), and the total is clamped so that stacking sources never
 * exceeds a ceiling. The numbers are ours: the fade runs from the source's full radius (3 m for an open
 * fire) to its zero radius (8 m), and the ceiling is the hottest single source's full bonus, so two Hot
 * fires never give more than one Hot fire beside you (+25 °C, the Blueprint's maximum).
 */
public final class HeatIndex {
    private HeatIndex() {}

    /** Straight-line fade, Cold Sweat's {@code blend}: {@code from} at or inside {@code rangeMin}, {@code to} at or past {@code rangeMax}. */
    static double blend(double from, double to, double factor, double rangeMin, double rangeMax) {
        if (factor <= rangeMin) return from;
        if (factor >= rangeMax) return to;
        return (to - from) / (rangeMax - rangeMin) * (factor - rangeMin) + from;
    }

    /** One source's contribution at its sample distance, after fade and occlusion. */
    public static double contribution(HeatSource s) {
        double faded = blend(s.bonusC(), 0, s.distanceM(), s.fullRadiusM(), s.zeroRadiusM());
        return faded / (s.occluders() + 1);
    }

    /** The heat bonus in °C for a set of sources: the sum of contributions, clamped to [0, hottest full bonus]. */
    public static double bonusC(List<HeatSource> sources) {
        if (sources == null || sources.isEmpty()) return 0;
        double sum = 0, ceiling = 0;
        for (HeatSource s : sources) {
            double c = contribution(s);
            if (c <= 0) continue;
            sum += c;
            ceiling = Math.max(ceiling, s.bonusC());
        }
        return Math.max(0, Math.min(sum, ceiling));
    }

    /**
     * True when the point is within {@code radiusM} of an unoccluded fire, stove or fireplace. The soaked
     * rule in Warmth ends "within 3 m of a fire".
     */
    public static boolean withinFire(List<HeatSource> sources, double radiusM) {
        if (sources == null) return false;
        for (HeatSource s : sources) {
            if (s.isFire() && s.occluders() == 0 && s.distanceM() <= radiusM && s.bonusC() > 0) return true;
        }
        return false;
    }
}
