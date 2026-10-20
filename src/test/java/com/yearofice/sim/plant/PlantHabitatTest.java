package com.yearofice.sim.plant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.plant.PlantCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every habitat tag of every plant file maps onto the cover, so no species is left unplaced. */
class PlantHabitatTest {
    static final List<String> CLASSES = List.of("spruce_taiga", "spruce_moss", "black_spruce_flats", "birch_slope", "lichen_woodland",
            "foothill_benches", "willow_flat", "muskeg", "alpine_tundra", "scree", "gravel_bar", "bar_edge", "cut_bank");

    @Test
    void everyTagIsKnownAndEverySpeciesHasAPlace() {
        var all = PlantHabitat.load(PlantCatalog.SPECIES);
        assertTrue(all.size() == PlantCatalog.SPECIES.length);
        for (var e : all) {
            for (String t : e.habitat()) assertTrue(PlantHabitat.known(t), e.id() + ": unknown habitat tag " + t);
            boolean placed = false;
            for (String c : CLASSES) placed |= PlantHabitat.fitsAny(e, new PlantHabitat.Site(c, 10, true, true, 0));
            assertTrue(placed, e.id() + " fits no cover class");
        }
    }

    @Test
    void water_and_rock_take_no_plants() {
        for (var e : PlantHabitat.load(PlantCatalog.SPECIES))
            for (String c : List.of("open_river", "lake", "rock_face")) assertFalse(PlantHabitat.fitsAny(e, new PlantHabitat.Site(c, 5, true, true, 0)));
    }
}
