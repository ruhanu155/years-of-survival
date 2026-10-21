package com.yearofice.sim.plant;

import java.util.Locale;

/** The eight phases of a plant block (Plants and fungi › Phases through the year). */
public enum PlantPhase {
    DORMANT, BURIED, BUD, LEAF, FLOWER, FRUIT, SEED, SENESCENT;

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public static PlantPhase of(String key) { return valueOf(key.toUpperCase(Locale.ROOT)); }
}
