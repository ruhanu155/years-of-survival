package com.yearofice.sim.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.sim.life.MigrationCalendar;
import com.yearofice.sim.life.MigrationDef;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.Climate;
import com.yearofice.sim.weather.ClimateJson;
import com.yearofice.sim.weather.DecadeStorm;
import com.yearofice.sim.weather.StormDef;
import com.yearofice.sim.weather.StormJson;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.sim.weather.StormMath;
import com.yearofice.sim.weather.StormTable;
import com.yearofice.sim.weather.WeatherModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SnowPack.SnowClass;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** M7-INT: storm water in the snow pack, and three simulated years back to back (storms + seasons + migration calendar). */
class YearIntegrationTest {
    static final Path DATA = Path.of("src/main/resources/data/yearofice");
    static final SeasonPreset P = SeasonPreset.STANDARD;
    static Climate climate;
    static final Map<String, StormDef> DEFS = new HashMap<>();
    static DecadeStorm decade;
    static MigrationCalendar cal;

    static JsonObject json(String p) throws IOException { return JsonParser.parseString(Files.readString(DATA.resolve(p))).getAsJsonObject(); }

    @BeforeAll
    static void load() throws IOException {
        climate = ClimateJson.parse(json("weather/interior_alaska.json")).climate();
        for (String id : List.of("snowstorm", "blizzard", "freezing_rain", "hail", "thunderstorm", "long_rain"))
            DEFS.put(id, StormJson.storm(json("weather/storm/" + id + ".json")));
        decade = DecadeStorm.parse(json("event/decade_storm.json"));
        List<MigrationDef> defs = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA.resolve("migration"))) {
            for (Path p : s.sorted().toList()) defs.add(MigrationDef.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject()));
        }
        cal = new MigrationCalendar(defs);
    }

    static StormTable table(long seed, int year, List<StormTable.Event> forced) {
        WeatherModel m = new WeatherModel(climate, seed);
        return StormTable.build(seed, year, DEFS.values(), StormJson.Ring.NONE, 1.0, d -> m.day(d).meanC(), decade, forced);
    }

    @Test
    void aBlizzardDayAddsSnowAndAJanuaryRainDayCrustsIt() {
        long day = 250; // deep winter
        long h0 = day * 24 + 2;
        StormTable blizz = table(5, Calendar.yearOfDay(day), List.of(new StormTable.Event("blizzard", StormKind.BLIZZARD, h0, 12, 1.0, true)));
        double[] w = StormMath.dayPrecip(blizz, DEFS, day, 1.0);
        assertTrue(w[0] > 3, "a 12 h blizzard drops snow water: " + w[0]);
        SeasonCore calm = new SeasonCore(), stormy = new SeasonCore();
        calm.step(day, 250 % 365, -25, 0, 3, P.snow, P.ice);
        stormy.step(day, 250 % 365, -25, 0, 3, w[0], w[1], P.snow, P.ice);
        assertTrue(stormy.snow(SnowClass.OPEN).depthCm() > calm.snow(SnowClass.OPEN).depthCm() + 15, "blizzard day adds snow");

        StormTable rain = table(5, Calendar.yearOfDay(day), List.of(new StormTable.Event("freezing_rain", StormKind.FREEZING_RAIN, h0, 8, 1.0, true)));
        double[] r = StormMath.dayPrecip(rain, DEFS, day, 1.0);
        assertTrue(r[1] > 0.5, "freezing rain is water: " + r[1]);
        SeasonCore c = new SeasonCore();
        for (int d = 0; d < 5; d++) c.step(day + d, 250, -25, 2, 3, P.snow, P.ice);
        assertTrue(!c.snow(SnowClass.OPEN).crust, "fresh powder has no crust");
        c.step(day + 5, 250, -6, 0, 3, 0, r[1], P.snow, P.ice);
        assertTrue(c.snow(SnowClass.OPEN).crust, "a January rain day crusts the pack");
    }

    @Test
    void threeYearsBackToBackOnFiveSeeds() {
        for (long seed : new long[] {1, 77, 3141592, 42, 2026}) {
            WeatherModel w = new WeatherModel(climate, seed);
            SeasonCore core = new SeasonCore();
            int[] freeze = {-1, -1, -1};
            boolean[] breakup = new boolean[3], salmon = new boolean[3];
            boolean[] floodSeen = new boolean[3];
            StormTable[] tables = {table(seed, 1, List.of()), table(seed, 2, List.of()), table(seed, 3, List.of())};
            for (int y = 0; y < 3; y++) {
                assertEquals(tables[y].hash(), table(seed, y + 1, List.of()).hash(), "table_replay year " + (y + 1));
                assertTrue(tables[y].events().size() > 5, "storms every year");
            }
            for (long d = 0; d < 365 * 3 + 60; d++) {
                int y = (int) Math.min(3, d / 365), doy = Calendar.dayOfYearOfDay(d);
                WeatherModel.Day day = w.day(d);
                double[] s = y > 2 ? new double[2] : StormMath.dayPrecip(tables[y], DEFS, d, 1.0);
                core.step(d, doy, day.meanC(), day.precipMm(), day.windMs(), s[0], s[1], P.snow, P.ice);
                var ev = core.lastEvent[BodyClass.RIVER.ordinal()];
                if (y < 3 && ev == IceModel.Event.FREEZE_UP && freeze[y] < 0 && doy > 100) freeze[y] = doy;
                int winter = (int) ((d - 100) / 365);
                if (d >= 100 && winter < 3 && ev == IceModel.Event.BREAKUP) breakup[winter] = true;
                if (d >= 100 && winter < 3 && core.floodM > 0) floodSeen[winter] = true;
                if (y < 3) for (MigrationDef m : cal.active(doy, 0)) if (m.kind().equals("salmon_run")) salmon[y] = true;
            }
            for (int y = 0; y < 3; y++) {
                assertTrue(freeze[y] >= 172 && freeze[y] <= 198, "seed " + seed + " year " + y + " froze doy " + freeze[y]);
                assertTrue(breakup[y] && floodSeen[y], "breakup and flood in spring " + y + ", seed " + seed);
                assertTrue(salmon[y], "salmon run in year " + y);
            }
        }
    }

    @Test
    void migrationWavesComeInOrder() {
        // the game year starts May 1, so in year order the waves run 3,4,5,6,7 then 1,2 (late winter and open water close the year)
        List<String> order = cal.all().stream().filter(d -> d.kind().equals("bird_wave")).sorted((a, b) -> Integer.compare(a.from(), b.from()))
                .map(d -> d.id().replaceAll(".*wave_(\\d).*", "$1")).toList();
        assertEquals(List.of("3", "4", "5", "6", "7", "1", "2"), order);
    }
}
