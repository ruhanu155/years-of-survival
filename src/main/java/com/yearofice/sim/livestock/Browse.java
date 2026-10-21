package com.yearofice.sim.livestock;

import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Goats and toxic plants (Plants and fungi › Goats and toxic plants). The tag {@code yearofice:goat_toxic} lists the
 * eight plants; this table says what each does. Any other toxic plant is refused: a goat does not touch it.
 */
public final class Browse {
    private Browse() {}

    public enum Effect {
        /** Dies within 6 game hours. */ DIES(6, 0, 0, 1.0),
        /** Milk 0 for 3 days, loses 30% of its condition. */ SICK(0, 3, 0.30, 0),
        /** Milk −50% for 2 days. */ DROOL(0, 2, 0, 0);

        public final int dieAfterHours, milkOffDays;
        public final double conditionLoss, lethal;

        Effect(int dieAfterHours, int milkOffDays, double conditionLoss, double lethal) {
            this.dieAfterHours = dieAfterHours;
            this.milkOffDays = milkOffDays;
            this.conditionLoss = conditionLoss;
            this.lethal = lethal;
        }

        /** Share of milk kept while the effect lasts. */
        public double milkFactor() { return this == DROOL ? 0.5 : 0; }
    }

    public static final Map<String, Effect> TOXIC = Map.of(
            "yearofice:monkshood", Effect.DIES, "yearofice:tall_larkspur", Effect.DIES, "yearofice:water_hemlock", Effect.DIES,
            "yearofice:false_hellebore", Effect.SICK, "yearofice:wild_calla", Effect.SICK, "yearofice:bog_rosemary", Effect.SICK,
            "yearofice:leatherleaf", Effect.DROOL, "yearofice:arctic_lupine", Effect.DROOL);

    /** The effect of eating this plant, or null when the goat refuses it (not on the goat_toxic list). */
    public static Effect effect(String plantId) { return TOXIC.get(plantId); }

    /** True when a goat eats the plant at all: only the listed ones are taken by mistake; other toxic plants are refused. */
    public static boolean eats(String plantId) { return TOXIC.containsKey(plantId); }

    /** A penned goat eats one listed clump within reach on 10% of days while one grows in its pen, and always from toxic hay. */
    public static boolean eatsToday(boolean clumpInReach, boolean toxicHay, RandomGenerator rng) {
        if (toxicHay) return true;
        return clumpInReach && rng.nextDouble() < 0.10;
    }

    /** Wet-meadow hay carries monkshood or larkspur in 5% of bundles unless the meadow was walked and weeded. */
    public static boolean hayBundleToxic(boolean meadowWeeded, RandomGenerator rng) { return !meadowWeeded && rng.nextDouble() < 0.05; }
}
