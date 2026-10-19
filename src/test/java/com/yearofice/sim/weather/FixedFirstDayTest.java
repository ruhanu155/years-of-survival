package com.yearofice.sim.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.sim.env.Precip;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Blueprint › Start site › Day 1 rules: May 1 fixed at cloudy, 7 °C high, −3 °C low, 2 m/s, dry; the chain starts from it. */
class FixedFirstDayTest {
    static Climate climate;

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/yearofice/weather/interior_alaska.json"), StandardCharsets.UTF_8);
        climate = ClimateJson.parse(JsonParser.parseString(json).getAsJsonObject()).climate();
    }

    @Test
    void dayOneIsTheBlueprintsDay() {
        WeatherModel w = new WeatherModel(climate, 77L);
        w.fixFirstDay(WeatherModel.FixedDay.BLUEPRINT_DAY_1);
        WeatherModel.Day d = w.day(0);
        assertEquals(DayState.CLOUDY, d.state());
        assertEquals(2.0, d.meanC(), 1e-9);
        assertEquals(0, d.precipMm(), 1e-9);
        double hi = -99, lo = 99;
        for (int h = 0; h < 24; h++) {
            double t = w.hourAirC(0, h);
            hi = Math.max(hi, t);
            lo = Math.min(lo, t);
            WeatherModel.Hour hour = w.hour(h * 60L);
            assertEquals(2.0, hour.windMs(), 1e-9, "wind at " + h);
            assertEquals(Precip.NONE, hour.precip(), "dry at " + h);
        }
        assertEquals(7.0, hi, 1e-9, "high");
        assertEquals(-3.0, lo, 1e-9, "low");
    }

    @Test
    void theChainStartsFromItAndLaterDaysKeepTheirRolls() {
        WeatherModel fixed = new WeatherModel(climate, 77L), drawn = new WeatherModel(climate, 77L);
        fixed.fixFirstDay(WeatherModel.FixedDay.BLUEPRINT_DAY_1);
        double keep = climate.anomalyKeep();
        // Day 2's anomaly carries day 1's: a_2 = keep · a_1 + the same noise roll in both worlds.
        double noiseFixed = fixed.day(1).anomalyC() - keep * fixed.day(0).anomalyC();
        double noiseDrawn = drawn.day(1).anomalyC() - keep * drawn.day(0).anomalyC();
        assertEquals(noiseDrawn, noiseFixed, 1e-9, "the noise roll is the same draw");
        assertNotEquals(drawn.day(0).meanC(), fixed.day(0).meanC());
        fixed.fixFirstDay(null);
        assertEquals(drawn.day(0).meanC(), fixed.day(0).meanC(), 1e-9, "unfixing redraws day 1");
        assertTrue(fixed.firstDay() == null);
    }
}
