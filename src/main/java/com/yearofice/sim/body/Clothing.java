package com.yearofice.sim.body;

/**
 * Clothing sums (Blueprint › Clothing).
 *
 * <pre>
 *   W_clothing = Σ_g w_g · c_g · (1 − m_g (1 − k_g))
 * </pre>
 *
 * Wind block is averaged over zones by their share; inside a zone two layers combine as
 * 1 − (1 − b_inner)(1 − b_outer). Reimplemented: Cold Sweat sums per-slot insulation the same way, but
 * its insulation multiplies temperature, while ours adds °C as the Blueprint sets.
 */
public final class Clothing {
    private Clothing() {}

    /** Bare skin is safe to −20 °C feels-like, −10 °C when wet. */
    public static final double BARE_SAFE_TO_C = -20, WET_SAFE_TO_C = -10;

    /** A zone counts as wet for frost limits and the body figure above 30% (the HUD's wet mark). */
    public static final double WET_MARK = 0.30;

    /** Wetness over 70% is soaked (the soaked rule). */
    public static final double SOAKED = 0.70;

    /** Summed clothing warmth in °C after condition and wetness. */
    public static double warmthC(ClothingState s) {
        double sum = 0;
        for (int i = 0; i < Layer.SLOTS; i++) {
            WornGarment w = s.at(i);
            if (w == null) continue;
            Garment g = w.garment();
            sum += g.warmthC() * w.conditionFactor() * (1 - s.wet(i) * (1 - g.kept()));
        }
        return sum;
    }

    /** The same sum with every garment dry: the "Clothing +27 °C" line under the body panel. */
    public static double dryWarmthC(ClothingState s) { return warmthC(s.withAllWet(0)); }

    public static double zoneWindBlock(ClothingState s, Zone z) {
        double pass = 1;
        for (Layer l : Layer.values()) {
            WornGarment w = s.at(z, l);
            if (w != null) pass *= 1 - w.effectiveWindBlock();
        }
        return 1 - pass;
    }

    /** Whole-body wind block, averaged over zones by share. */
    public static double windBlock(ClothingState s) {
        double b = 0;
        for (Zone z : Zone.values()) b += z.share * zoneWindBlock(s, z);
        return b;
    }

    /** A zone's wetness: the wetter of its two slots. */
    public static double zoneWetness(ClothingState s, Zone z) {
        return Math.max(s.wet(z, Layer.INNER), s.wet(z, Layer.OUTER));
    }

    /** Overall w for the feels-like formula: zone wetness averaged by share. */
    public static double wetness(ClothingState s) {
        double w = 0;
        for (Zone z : Zone.values()) w += z.share * zoneWetness(s, z);
        return w;
    }

    public static boolean soaked(ClothingState s) { return wetness(s) > SOAKED; }

    /**
     * The zone's "safe to" limit: the best limit among its garments, bare skin's −20 °C otherwise, and the
     * wet limit −10 °C whenever the zone is wet, whatever is worn.
     */
    public static double zoneSafeToC(ClothingState s, Zone z) {
        if (zoneWetness(s, z) > WET_MARK) return WET_SAFE_TO_C;
        double limit = BARE_SAFE_TO_C;
        for (Layer l : Layer.values()) {
            WornGarment w = s.at(z, l);
            if (w != null && !Double.isNaN(w.garment().safeToC())) limit = Math.min(limit, w.garment().safeToC());
        }
        return limit;
    }

    /** Extra kg of water carried in worn clothing: Σ weight × uptake × wetness (Waters › Wet clothes weigh). */
    public static double waterWeightKg(ClothingState s) {
        double kg = 0;
        for (int i = 0; i < Layer.SLOTS; i++) {
            WornGarment w = s.at(i);
            if (w != null) kg += w.garment().weightKg() * w.garment().waterUptake() * s.wet(i);
        }
        return kg;
    }
}
