package com.yearofice.sim.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.sim.time.Calendar;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Weather and storms › Storm catalogue, Storm of the decade, Deterministic and replayable. */
class StormTableTest {
    static JsonObject json(String path) {
        try (var in = StormTableTest.class.getResourceAsStream("/data/yearofice/" + path)) {
            return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static final List<StormDef> DEFS = new ArrayList<>();

    static {
        for (String id : List.of("snowstorm", "blizzard", "freezing_rain", "hail", "thunderstorm", "long_rain"))
            DEFS.add(StormJson.storm(json("weather/storm/" + id + ".json")));
    }

    static final DecadeStorm DECADE = DecadeStorm.parse(json("event/decade_storm.json"));
    static final Climate CLIMATE = com.yearofice.sim.weather.ClimateJson.parse(json("weather/interior_alaska.json")).climate();

    static StormTable table(long seed, int year) {
        WeatherModel m = new WeatherModel(CLIMATE, seed);
        return StormTable.build(seed, year, DEFS, StormJson.Ring.NONE, 1.0, d -> m.day(d).meanC(), DECADE, List.of());
    }

    @Test
    void decadeStormLandsWithin5PointsOfItsChanceOver100Years() {
        double sum = 0;
        int seeds = 40;
        for (int s = 1; s <= seeds; s++) {
            var draws = DECADE.simulate(s * 7919L, 100);
            sum += draws.size() / 100.0;
            for (int i = 1; i < draws.size(); i++) assertTrue(draws.get(i).year() - draws.get(i - 1).year() >= 7, "one in any 7 years");
            for (var d : draws) {
                assertTrue(d.year() >= 2);
                int doy = (int) ((d.startHour() / 24) % Calendar.DAYS_PER_YEAR);
                assertTrue(doy >= d.variant().fromDoy() && doy <= d.variant().toDoy(), d.variant().id() + " inside its window");
            }
        }
        assertEquals(DECADE.chancePerYear(), sum / seeds, 0.05);
    }

    @Test
    void stormsStartOnlyInTheirMonthsAndNeverOverlap() {
        for (int year = 1; year <= 12; year++) {
            StormTable t = table(42, year);
            StormTable.Event prev = null;
            for (var e : t.events()) {
                int month = Calendar.month((int) ((e.startHour() / 24) % Calendar.DAYS_PER_YEAR));
                switch (e.id()) {
                    case "thunderstorm", "hail" -> assertTrue(month >= 6 && month <= 8, e.id() + " in month " + month);
                    case "blizzard" -> assertTrue(month >= 11 || month <= 3, "blizzard in month " + month);
                    case "freezing_rain" -> assertTrue(month != 6 && month != 7 && month != 8 && month != 5 && month != 9, "glaze in month " + month);
                    default -> { }
                }
                if (prev != null && !e.id().equals("long_rain")) assertTrue(e.startHour() >= prev.endHour() || prev.id().equals("long_rain"), "no overlap");
                prev = e;
            }
        }
    }

    @Test
    void yearsAreAboutRight() {
        int thunder = 0, blizzard = 0;
        int years = 60;
        for (int y = 1; y <= years; y++)
            for (var e : table(7, y).events()) {
                if (e.id().equals("thunderstorm")) thunder++;
                if (e.id().equals("blizzard")) blizzard++;
            }
        assertEquals(11, thunder / (double) years, 4, "about 11 thunder days a year");
        assertEquals(4, blizzard / (double) years, 3, "3-5 snowing blizzards a year");
    }

    @Test
    void sameSeedSameTableDifferentSeedDifferent() {
        assertEquals(table(5, 3).hash(), table(5, 3).hash());
        assertNotEquals(table(5, 3).hash(), table(6, 3).hash());
    }

    @Test
    void glazeNumbers() {
        assertEquals(0, StormMath.glazeLayers(1.9));
        assertEquals(1, StormMath.glazeLayers(2));
        assertEquals(2, StormMath.glazeLayers(10));
        assertEquals(3, StormMath.glazeLayers(40));
        assertEquals(0.7 * 10, StormMath.glazeMm(10, false), 1e-9);
        assertEquals(0.7 * 10 * 0.3, StormMath.glazeMm(10, true), 1e-9);
    }
}
