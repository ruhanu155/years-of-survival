package com.yearofice.sim.build;

/**
 * Ore to bloom to bar (Crafting and building in depth › Tier 4: Iron). Ore is counted in quarter-units of a
 * nodule: a nodule or a piece of bog iron is 40 quarters, scrap iron 20, one 100 g unit of black sand 1 (40 units =
 * 4 kg = one 3 kg nodule, video 2 A10). A smelt takes 5 nodules (200 quarters) and 20 kg of charcoal for one 3 kg
 * bloom; consolidating takes the bloom and 3 kg of charcoal for 2.0 kg of bar (2.5 kg when Practiced). Pure Java;
 * original Year of Ice code.
 */
public final class Smelt {
    private Smelt() {}

    public static final int NODULE = 40, BOG_IRON = 40, SCRAP = 20, SAND_UNIT = 1;
    public static final int QUARTERS_PER_BLOOM = 5 * NODULE;
    public static final int CHARCOAL_PER_BLOOM = 20;
    public static final int CHARCOAL_PER_CONSOLIDATION = 3;
    public static final int BLOOM_KG = 3;
    /** Bar iron in 0.5 kg bars: 2.0 kg (4 bars), or 2.5 kg (5) once Practiced. */
    public static final int BAR_UNITS = 4, BAR_UNITS_PRACTICED = 5;

    /** Blooms a charge makes: whole smelts of ore and of charcoal, whichever runs out first. */
    public static int blooms(int quarters, int charcoalKg) {
        return Math.max(0, Math.min(quarters / QUARTERS_PER_BLOOM, charcoalKg / CHARCOAL_PER_BLOOM));
    }

    public static int barUnits(boolean practiced) { return practiced ? BAR_UNITS_PRACTICED : BAR_UNITS; }

    /** Sluice box output against the pan: the same black sand per hour times the multiplier. */
    public static double sluiceKgPerHour(double panKgPerHour, double mult) { return panKgPerHour * mult; }
}
