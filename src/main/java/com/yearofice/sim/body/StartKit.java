package com.yearofice.sim.body;

import static com.yearofice.sim.body.Layer.INNER;
import static com.yearofice.sim.body.Layer.OUTER;

/**
 * The starting clothes (Blueprint › Clothing, Start tier) as the body slice's stand-in for worn items, until the
 * clothing items land: fleece, rain shell, work pants, wool socks, hiking boots, wool beanie, work gloves (the base
 * layer shares the fleece's slot and stays in the pack). The dry sum is scaled to the preset's {@code kit_warmth_c}
 * dial so the whole kit is one tuning number; the Blueprint's table sums to {@link #TABLE_C}.
 */
public final class StartKit {
    private StartKit() {}

    /** The garment table's own dry sum for the worn set. */
    public static final double TABLE_C = 15;

    private static final double NA = Double.NaN;

    public static ClothingState wear(ClothingState base, double kitWarmthC) {
        if (kitWarmthC <= 0) return base; // no kit dial, no clothes (bare-skin tests)
        double k = kitWarmthC / TABLE_C;
        Garment[] set = {
                new Garment("fleece_jacket", Zone.TORSO, INNER, 5 * k, 0.1, 0.7, NA, false, 0.5, 0.4, false),
                new Garment("rain_shell", Zone.TORSO, OUTER, 1 * k, 0.9, NA, NA, true, 0.4, 0.8, false),
                new Garment("work_pants", Zone.LEGS, INNER, 2 * k, 0.2, 0.3, NA, false, 0.6, 0.8, false),
                new Garment("wool_socks", Zone.FEET, INNER, 2 * k, 0, 0.8, NA, false, 0.1, 1.5, false),
                new Garment("hiking_boots", Zone.FEET, OUTER, 2 * k, 0.5, NA, -22, true, 1.4, 0.8, false),
                new Garment("wool_beanie", Zone.HEAD, INNER, 2 * k, 0.3, 0.8, NA, false, 0.1, 1.5, false),
                new Garment("work_gloves", Zone.HANDS, INNER, 1 * k, 0.2, 0.2, -22, false, 0.1, 0.8, false)};
        ClothingState s = base;
        for (Garment g : set) s = s.wear(WornGarment.fresh(g));
        return s;
    }
}
