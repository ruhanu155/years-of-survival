package com.yearofice.sim.body;

/**
 * A garment's wear data, the pure view of a {@code data_maps/item/wear.json} entry
 * ({@code warmth_c}, {@code wind_block}, {@code kept_when_wet}, {@code safe_to_c}, {@code water_uptake}).
 *
 * @param keptWhenWet share of warmth kept when soaked; {@code NaN} means "not given", which uses the
 *                    Blueprint's default 0.3 (the 0.7 factor in the feels-like formula).
 * @param safeToC     the zone's "safe to" temperature with this garment on; {@code NaN} when it sets none.
 * @param waterproof  sheds rain and puddles while condition is 50% or more (rain shell, hiking boots).
 * @param waterUptake water weight per kg of garment when soaked (Waters › Wet clothes weigh).
 */
public record Garment(String id, Zone zone, Layer layer, double warmthC, double windBlock, double keptWhenWet,
                      double safeToC, boolean waterproof, double weightKg, double waterUptake, boolean fur) {

    public static final double DEFAULT_KEPT_WHEN_WET = 0.3;

    public Garment {
        if (zone == null || layer == null) throw new IllegalArgumentException("zone and layer");
        if (windBlock < 0 || windBlock > 1) throw new IllegalArgumentException("wind block 0–1");
    }

    public double kept() { return Double.isNaN(keptWhenWet) ? DEFAULT_KEPT_WHEN_WET : keptWhenWet; }

    public int slot() { return Layer.slot(zone, layer); }
}
