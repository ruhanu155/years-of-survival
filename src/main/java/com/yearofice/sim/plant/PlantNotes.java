package com.yearofice.sim.plant;

import java.util.Map;
import java.util.TreeMap;

/**
 * The plant notebook (Blueprint › Foraging and plant knowledge): each species is Unknown, Partly known or Identified.
 * Four ways in: the pocket guide, a logbook or field-note page, seeing the shoot, flower and fruit stages (a taste test
 * or a herbarium sheet also identifies). An immutable value; the player data holds one.
 */
public final class PlantNotes {
    public enum Level { UNKNOWN, PARTLY, IDENTIFIED }

    public static final int SHOOT = 1, FLOWER = 2, FRUIT = 4, ALL_STAGES = 7;
    public static final PlantNotes EMPTY = new PlantNotes(Map.of());

    private final Map<String, Integer> entries; // species -> stage bits, or -1 for identified

    public PlantNotes(Map<String, Integer> entries) { this.entries = Map.copyOf(entries); }

    public Map<String, Integer> raw() { return entries; }

    public Level level(String species) {
        Integer v = entries.get(species);
        if (v == null || v == 0) return Level.UNKNOWN;
        return v == -1 || (v & ALL_STAGES) == ALL_STAGES ? Level.IDENTIFIED : Level.PARTLY;
    }

    public boolean identified(String species) { return level(species) == Level.IDENTIFIED; }

    /** Inspecting {@code species} at a growth stage bit; the third stage identifies it. */
    public PlantNotes seen(String species, int stageBit) {
        Integer v = entries.getOrDefault(species, 0);
        if (v == -1) return this;
        return with(species, v | stageBit);
    }

    /** Pocket guide, logbook page, field note, herbarium sheet or a safe taste test. */
    public PlantNotes identify(String species) { return with(species, -1); }

    public PlantNotes forget(String species) {
        TreeMap<String, Integer> m = new TreeMap<>(entries);
        m.remove(species);
        return new PlantNotes(m);
    }

    private PlantNotes with(String species, int v) {
        TreeMap<String, Integer> m = new TreeMap<>(entries);
        m.put(species, v);
        return new PlantNotes(m);
    }

    /** What the crosshair shows: the name for an identified plant, "a plant" otherwise. */
    public String shownName(String species, String name) { return identified(species) ? name : "a plant"; }

    public int stagesSeen(String species) {
        Integer v = entries.get(species);
        return v == null ? 0 : v == -1 ? ALL_STAGES : v & ALL_STAGES;
    }
}
