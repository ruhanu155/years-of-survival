package com.yearofice.sim.body;

import java.util.List;

/** What a player really wears (M7 lane F): the worn garments plus the per-slot wetness, as the body's {@link ClothingState}. */
public final class Outfit {
    private Outfit() {}

    /** One garment per slot (a later one in {@code worn} replaces an earlier one in the same slot); wet is the 10 slot values. */
    public static ClothingState dress(List<WornGarment> worn, double[] slotWet) {
        ClothingState s = ClothingState.naked().withWet(slotWet);
        for (WornGarment w : worn) s = s.wear(w);
        return s;
    }

    /** The garment table's start set (data rows {@code yearofice:fleece_jacket} ... ), for the equip-on-new-game list. */
    public static final List<String> START_IDS = List.of("fleece_jacket", "rain_shell", "work_pants", "wool_socks",
            "hiking_boots", "wool_beanie", "work_gloves");
}
