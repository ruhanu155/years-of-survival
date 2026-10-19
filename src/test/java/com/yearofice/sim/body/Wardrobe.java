package com.yearofice.sim.body;

import static com.yearofice.sim.body.Layer.INNER;
import static com.yearofice.sim.body.Layer.OUTER;

/**
 * The Blueprint › Clothing garment table, as test fixtures. In the game these rows are
 * {@code data_maps/item/wear.json} entries; tests keep their own copy so a data edit cannot quietly move
 * a blueprint test.
 */
final class Wardrobe {
    private Wardrobe() {}

    static final double NA = Double.NaN;

    static Garment g(String id, Zone z, Layer l, double warmth, double wind, double kept, double safeTo,
                     boolean waterproof, double kg, double uptake, boolean fur) {
        return new Garment(id, z, l, warmth, wind, kept, safeTo, waterproof, kg, uptake, fur);
    }

    // Start
    static final Garment BASE_LAYER = g("synthetic_base_layer", Zone.TORSO, INNER, 3, 0, 0.8, NA, false, 0.2, 0.4, false);
    static final Garment FLEECE = g("fleece_jacket", Zone.TORSO, INNER, 5, 0.1, 0.7, NA, false, 0.5, 0.4, false);
    static final Garment RAIN_SHELL = g("rain_shell", Zone.TORSO, OUTER, 1, 0.9, NA, NA, true, 0.4, 0.8, false);
    static final Garment WORK_PANTS = g("work_pants", Zone.LEGS, INNER, 2, 0.2, 0.3, NA, false, 0.6, 0.8, false);
    static final Garment WOOL_SOCKS = g("wool_socks", Zone.FEET, INNER, 2, 0, 0.8, NA, false, 0.1, 1.5, false);
    static final Garment HIKING_BOOTS = g("hiking_boots", Zone.FEET, OUTER, 2, 0.5, NA, -22, true, 1.4, 0.8, false);
    static final Garment WOOL_BEANIE = g("wool_beanie", Zone.HEAD, INNER, 2, 0.3, 0.8, NA, false, 0.1, 1.5, false);
    static final Garment WORK_GLOVES = g("work_gloves", Zone.HANDS, INNER, 1, 0.2, 0.2, -22, false, 0.1, 0.8, false);
    static final Garment HEAD_NET = g("mosquito_head_net", Zone.HEAD, OUTER, 0, 0, NA, NA, false, 0.05, 0, false);
    // Hide and bushcraft
    static final Garment MOCCASINS = g("moccasins", Zone.FEET, OUTER, 1, 0.3, 0.3, NA, false, 0.4, 1.0, false);
    static final Garment PONCHO = g("small_pelt_poncho", Zone.TORSO, OUTER, 5, 0.4, 0.6, NA, false, 1.5, 1.0, true);
    static final Garment HIDE_COAT = g("hide_coat", Zone.TORSO, OUTER, 12, 0.9, 0.5, NA, false, 3.5, 1.0, false);
    static final Garment PARKA = g("parka", Zone.TORSO, OUTER, 15, 0.95, 0.5, NA, false, 4, 1.0, true);
    static final Garment HIDE_PANTS = g("hide_pants", Zone.LEGS, OUTER, 8, 0.9, 0.5, NA, false, 2.5, 1.0, false);
    static final Garment FUR_HAT = g("fur_hat", Zone.HEAD, OUTER, 4, 0.8, 0.6, -30, false, 0.4, 1.0, true);
    static final Garment FUR_MITTENS = g("fur_mittens", Zone.HANDS, OUTER, 4, 0.9, 0.5, -25, false, 0.3, 1.0, true);
    static final Garment HARE_LINERS = g("hare_fur_liners", Zone.FEET, INNER, 3, 0, 0.6, NA, false, 0.2, 1.0, true);
    static final Garment MUKLUKS = g("mukluks", Zone.FEET, OUTER, 6, 0.9, 0.4, -35, false, 1.2, 1.0, true);

    /*
     * The Blueprint lists both the synthetic base layer and the fleece as "Torso, inner", but a zone has one
     * inner slot. Until the Director rules, the sets below wear the fleece in that slot and leave the base
     * layer in the pack.
     */

    /** May to August: the starting clothes. */
    static ClothingState start() {
        return ClothingState.of(FLEECE, RAIN_SHELL, WORK_PANTS, WOOL_SOCKS, HIKING_BOOTS, WOOL_BEANIE,
                WORK_GLOVES);
    }

    /** October to November: hide coat, hide pants, fur hat, mittens over the starting inner layers. */
    static ClothingState october() {
        return ClothingState.of(FLEECE, HIDE_COAT, WORK_PANTS, HIDE_PANTS, WOOL_SOCKS, HIKING_BOOTS,
                WOOL_BEANIE, FUR_HAT, WORK_GLOVES, FUR_MITTENS);
    }
}
