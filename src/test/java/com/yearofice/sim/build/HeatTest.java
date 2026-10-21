package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.LongFunction;
import org.junit.jupiter.api.Test;

class HeatTest {
    static final double LOG = 0.9, ROOF = 0.5, FLOOR = 0.8;

    /** A 4x4 interior, 2 high, log walls, pole-and-sod roof, slab floor; optionally one 4x2 wall missing. */
    static LongFunction<RoomScan.Cell> cabin(boolean missingWall) {
        return p -> {
            int x = RoomScan.ux(p), y = RoomScan.uy(p), z = RoomScan.uz(p);
            boolean in = x >= 0 && x < 4 && z >= 0 && z < 4 && y >= 0 && y < 2;
            if (in) return RoomScan.Cell.AIR;
            if (y == -1 && x >= 0 && x < 4 && z >= 0 && z < 4) return RoomScan.Cell.solid(FLOOR);
            if (y == 2 && x >= 0 && x < 4 && z >= 0 && z < 4) return RoomScan.Cell.solid(ROOF);
            if (y >= 0 && y < 2 && ((x == -1 || x == 4) && z >= 0 && z < 4 || (z == -1 || z == 4) && x >= 0 && x < 4)) {
                if (missingWall && z == 4) return RoomScan.Cell.AIR;
                return RoomScan.Cell.solid(LOG);
            }
            return RoomScan.Cell.AIR;
        };
    }

    @Test
    void closed_cabin_with_a_4kw_stove_holds_above_5c_at_minus_30() {
        RoomScan.Result r = RoomScan.scan(cabin(false), 1, 0, 1, 600);
        assertTrue(r.enclosed());
        assertEquals(32, r.volume());
        Heat.Balance b = Heat.solve(r.faces(), 0, 0, 4000, -30, -5, 3);
        assertTrue(b.feelsLikeC() > 5, "was " + b.feelsLikeC());
    }

    @Test
    void cabin_missing_a_wall_is_not_a_room_and_does_not_hold() {
        assertFalse(RoomScan.scan(cabin(true), 1, 0, 1, 600).enclosed());
        // The same envelope with 8 m2 open to the outside.
        RoomScan.Result r = RoomScan.scan(cabin(false), 1, 0, 1, 600);
        Heat.Balance b = Heat.solve(r.faces(), 8, 0, 4000, -30, -5, 3);
        assertTrue(b.feelsLikeC() < 5, "was " + b.feelsLikeC());
    }

    @Test
    void roof_snow_insulates_and_wind_costs() {
        Heat.Face bare = new Heat.Face(Heat.Part.ROOF, 16, 0.5, 0), snowy = new Heat.Face(Heat.Part.ROOF, 16, 0.5, 0.4);
        assertTrue(Heat.effectiveU(snowy, 0) < Heat.effectiveU(bare, 0));
        assertTrue(Heat.effectiveU(bare, 10) > Heat.effectiveU(bare, 0));
    }

    @Test
    void stamping_takes_under_10_percent_of_the_first_build() {
        List<Stamp.Piece> pieces = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) pieces.add(new Stamp.Piece("yearofice:wall_log", 12));
        for (int i = 0; i < 16; i++) pieces.add(new Stamp.Piece("yearofice:roof_pole_sod", 20));
        double first = Stamp.firstBuildMin(pieces);
        assertTrue(Stamp.stampMin(pieces, 0.05, 5) < 0.1 * first);
    }

    /** The proxy's 30 m3 cabin (about 72 W/C) on an 8 kW STEADY tin stove at -30 C reads +25..+35 C once the chimney draw counts; it read +86 C without. */
    @Test
    void steadyTinStoveCabinReadsTwentyFiveToThirtyFive() {
        var faces = List.of(new Heat.Face(Heat.Part.WALL, 66, 0.9, 0), new Heat.Face(Heat.Part.FLOOR, 10, 0.8, 0));
        double without = Heat.solve(faces, 0, 0, 8091, -30, -5, 0).insideC();
        double with = Heat.solve(faces, 0, 0, 8091, 8091, -30, -5, 0).insideC();
        assertTrue(without > 60, "no draw: " + without);
        assertTrue(with >= 25 && with <= 35, "with the draw: " + with);
    }
}
