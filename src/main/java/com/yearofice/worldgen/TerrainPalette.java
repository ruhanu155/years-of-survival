package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * The world blocks the column builder writes, by name ({@code yearofice:<name>}, except {@code water}). Pure: the
 * chunk generator resolves each index to a block state once, so the column builder only handles small ints.
 * The fixed names come first; the soils and surfaces the {@code cover} list names follow in first-use order.
 */
public final class TerrainPalette {
    public static final int AIR = 0, WATER = 1, DEEP_GROUND = 2, DEEP_ROCK = 3, FACE_ROCK = 4, IRON_OUTCROP = 5,
            COAL_SEAM = 6, RUST_STAIN = 7, FROZEN_SOIL = 8, RIFFLE_COBBLE = 9, GRAVEL = 10;
    static final String[] FIXED = {"air", "water", "deep_ground", "deep_rock", "face_rock", "iron_outcrop", "coal_seam",
            "rust_stain", "frozen_soil", "riffle_cobble", "gravel"};

    private final List<String> names = new ArrayList<>(List.of(FIXED));

    /** Index of a name, adding it if new. */
    int index(String name) {
        int k = names.indexOf(name);
        if (k >= 0) return k;
        names.add(name);
        return names.size() - 1;
    }

    public List<String> names() { return List.copyOf(names); }

    public String name(int index) { return names.get(index); }

    public int size() { return names.size(); }
}
