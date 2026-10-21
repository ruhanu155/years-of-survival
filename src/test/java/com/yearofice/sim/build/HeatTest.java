package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.fire.Stove;
import com.yearofice.sim.fire.StoveBurn;
import com.yearofice.sim.fire.Wood;
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

    /**
     * Warm rooms, "the first cabin, worked through": 30 m3 of chinked log walls (U 0.5, 33 m2), a sod roof (0.4, 15 m2) and a
     * plank floor, on 2 armloads a day in a stove that keeps 85 % (the barrel stove) is 20 kWh a day, 836 W on average over
     * the day (8 fired hours of 24), and settles +25..+35 C above a -30 C day. Chinked seams add nothing; unchinked, the same
     * walls leak 3.3 m of seam a block at 0.4 W/C per metre and the 2 armloads reach about half that.
     */
    @Test
    void chinkedCabinOnTwoArmloadsADayReadsTwentyFiveToThirtyFive() {
        var faces = List.of(new Heat.Face(Heat.Part.WALL, 33, 0.5, 0), new Heat.Face(Heat.Part.ROOF, 15, 0.4, 0), new Heat.Face(Heat.Part.FLOOR, 15, 0.45, 0));
        double meanW = 2 * 11.8 * 1000 * 0.85 / 24;
        double chinked = Heat.solve(faces, 0, 0, meanW, -30, -5, 0).insideC() + 30;
        double drafty = Heat.solve(faces, 0, 33 * 3.3, meanW, -30, -5, 0).insideC() + 30;
        assertTrue(chinked >= 25 && chinked <= 35, "chinked cabin is +" + chinked + " C above outside");
        assertTrue(drafty < chinked * 0.6, "drafty cabin is +" + drafty + " C");
    }

    /** Hot is three times the burn and half of it goes up the chimney: 1.5 x Steady's heat, not 3 x. */
    @Test
    void hotStoveGivesOneAndAHalfTimesSteadyHeat() {
        var kind = new Stove.Kind("barrel_stove", 150, 0.7, 3, 1.5, 3.5);
        double steady = Stove.outputW(kind, com.yearofice.sim.env.FireSetting.STEADY, 1.0, Wood.BIRCH, 15);
        double hot = Stove.outputW(kind, com.yearofice.sim.env.FireSetting.HOT, 1.0, Wood.BIRCH, 15);
        assertEquals(1.5, hot / steady, 1e-9);
    }

    /** Duty from the fuel state: fresh light 1, 8 fired hours of 24 is a third, a day of firing 1. */
    @Test
    void dutyCycleFromTheFuelState() {
        var kind = new Stove.Kind("barrel_stove", 240, 0.85, 3, 1.5, 3.5);
        var b = new StoveBurn(kind);
        b.load(Wood.BIRCH, 15, 2.0);
        assertEquals(2 * 240 * Wood.BIRCH.burnFactor / 480.0 > 1 ? 1.0 : 2 * 240 * Wood.BIRCH.burnFactor / 480.0, b.duty(1.0), 1e-9); // fresh: queued fuel over 8 h
        b.firedMin24(480);
        b.observedMin24(1440);
        assertEquals(1.0 / 3, b.duty(1.0), 1e-9); // 8 fired hours of the last 24
        b.firedMin24(1440);
        assertEquals(1.0, b.duty(1.0), 1e-9);
    }
}
