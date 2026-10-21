package com.yearofice.sim.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The stovehouse grows greens in winter on the room's air. */
class StovehouseTest {
    @Test
    void a_heated_room_grows_winter_greens_the_open_does_not() {
        double inside = Stovehouse.growthUnits(15, 6, Stovehouse.Glazing.GLASS);
        assertTrue(inside > 3.0, "units a day at 15 C and 6 h of light: " + inside);
        assertEquals(0.0, Stovehouse.growthUnits(-20, 6, Stovehouse.Glazing.GLASS), 1e-9);
        double days = Stovehouse.daysToRipe(700, 15, 6, Stovehouse.Glazing.GLASS);
        assertTrue(days > 100 && days < 250, "a turnip ripens in a winter, not a week: " + days);
        assertTrue(Stovehouse.growthUnits(15, 14, Stovehouse.Glazing.GLASS) > inside, "longer days grow faster");
    }

    @Test
    void glazing_scales_the_light() {
        double glass = Stovehouse.growthUnits(18, 12, Stovehouse.Glazing.GLASS);
        assertEquals(0.9 * glass, Stovehouse.growthUnits(18, 12, Stovehouse.Glazing.PLEXIGLASS), 1e-9);
        assertEquals(0.5 * glass, Stovehouse.growthUnits(18, 12, Stovehouse.Glazing.RAWHIDE), 1e-9);
        assertTrue(Stovehouse.killsGreens(-6) && !Stovehouse.killsGreens(2));
        assertEquals(-1.0, Stovehouse.daysToRipe(700, 3, 12, Stovehouse.Glazing.GLASS), 1e-9, "a cold room never ripens");
    }
}
