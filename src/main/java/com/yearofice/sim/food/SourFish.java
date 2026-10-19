package com.yearofice.sim.food;

import com.yearofice.sim.decay.Thresholds;

/**
 * Sour fish and botulism (Decay and time › Sour fish and botulism).
 *
 * <p>Fish heads, eggs or whole small fish fermented cool in a grass-lined pit or a birch-bark basket become
 * sour fish: 10 game days at 3–10 °C, keeps 20 days, 115% of the parts' calories. Sealed airtight, each batch
 * rolls 10% toxic; a grass pit or bark basket never does, because air reaches it. Boiling a batch 10 game
 * minutes destroys the toxin. The roll is a threshold drawn from the batch's position and start day, so it
 * never depends on when anyone looked.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class SourFish {
    private SourFish() {}

    public static final double FERMENT_DAYS = 10.0;
    public static final double FERMENT_MIN_C = 3.0;
    public static final double FERMENT_MAX_C = 10.0;
    public static final double KEEPS_DAYS = 20.0;
    public static final double CALORIE_FACTOR = 1.15;
    public static final double SEALED_TOXIC_CHANCE = 0.10;
    public static final double BOIL_MINUTES_TO_DESTROY = 10.0;

    public enum Container {
        GRASS_PIT(false), BARK_BASKET(false), LIDDED_TIN(true), GLASS_JAR(true), JERRYCAN(true), PLASTIC(true);

        private final boolean airtight;

        Container(boolean airtight) { this.airtight = airtight; }

        public boolean airtight() { return airtight; }
    }

    /** True if a batch started at this position and day is toxic. */
    public static boolean toxic(long seed, int x, int y, int z, long startDay, Container container) {
        if (!container.airtight()) return false;
        return Thresholds.stream(seed, "sour_fish", x, y, z, startDay).nextDouble() < SEALED_TOXIC_CHANCE;
    }

    /** Botulism from eating a batch: toxic and not boiled long enough. */
    public static boolean causesBotulism(boolean toxic, double boiledMinutes) {
        return toxic && boiledMinutes < BOIL_MINUTES_TO_DESTROY;
    }

    /** True while the batch's store sits in the fermenting band. */
    public static boolean fermenting(double tempC) { return tempC >= FERMENT_MIN_C && tempC <= FERMENT_MAX_C; }
}
