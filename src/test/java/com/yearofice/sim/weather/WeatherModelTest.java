package com.yearofice.sim.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.sim.weather.ClimateJson;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The M2 weather draw against the climate table: normals, totals, determinism and the water rule. */
class WeatherModelTest {
    static Climate climate;
    static final int YEARS = 40;

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/yearofice/weather/interior_alaska.json"), StandardCharsets.UTF_8);
        climate = ClimateJson.parse(JsonParser.parseString(json).getAsJsonObject()).climate();
    }

    /** Over 40 years each month's mean air is its normal within 1 °C, and hourly means too (the curve is centred). */
    @Test
    void monthlyMeansHoldTheNormals() {
        WeatherModel w = new WeatherModel(climate, 3141592L);
        double[] sum = new double[12], hourly = new double[12];
        int[] n = new int[12];
        for (long d = 0; d < YEARS * 365L; d++) {
            int m = Calendar.month(Calendar.dayOfYearOfDay(d));
            sum[m - 1] += w.day(d).meanC();
            for (int h = 0; h < 24; h += 3) hourly[m - 1] += w.hourAirC(d, h) / 8;
            n[m - 1]++;
        }
        for (int m = 1; m <= 12; m++) {
            assertEquals(climate.meanC()[m - 1], sum[m - 1] / n[m - 1], 1.0, "day means, month " + m);
            assertEquals(climate.meanC()[m - 1], hourly[m - 1] / n[m - 1], 1.2, "hourly means, month " + m);
        }
    }

    /** Expected precipitation equals the normals: the 40-year mean year is 275 mm within 12%. */
    @Test
    void precipitationTotals() {
        WeatherModel w = new WeatherModel(climate, 77L);
        double mm = 0, snowDaysJan = 0;
        for (long d = 0; d < YEARS * 365L; d++) {
            WeatherModel.Day day = w.day(d);
            mm += day.precipMm();
            if (Calendar.month(Calendar.dayOfYearOfDay(d)) == 1 && day.precipMm() > 0) snowDaysJan++;
        }
        double perYear = mm / YEARS;
        assertEquals(275, perYear, 275 * 0.12, "mm a year");
        assertTrue(snowDaysJan > 0);
        // A January precipitation hour is snow; a July one is rain or drizzle.
        boolean sawSnow = false, sawRain = false;
        for (long d = 0; d < 3 * 365L; d++) {
            int m = Calendar.month(Calendar.dayOfYearOfDay(d));
            for (int h = 0; h < 24; h++) {
                WeatherModel.Hour hr = w.hour(d * 1440 + h * 60);
                if (hr.precip() == Precip.NONE) continue;
                if (m == 1) {
                    assertEquals(Precip.SNOW, hr.precip());
                    sawSnow = true;
                }
                if (m == 7) {
                    assertTrue(hr.precip() == Precip.RAIN || hr.precip() == Precip.DRIZZLE, "July " + hr.precip());
                    sawRain = true;
                }
            }
        }
        assertTrue(sawSnow && sawRain);
    }

    @Test
    void deterministicAndSeeded() {
        WeatherModel a = new WeatherModel(climate, 42), b = new WeatherModel(climate, 42), c = new WeatherModel(climate, 43);
        int differ = 0;
        for (long d = 0; d < 800; d += 7) {
            assertEquals(a.day(d), b.day(d));
            if (!a.day(d).equals(c.day(d))) differ++;
        }
        assertTrue(differ > 100);
        // Reading out of order gives the same days.
        WeatherModel late = new WeatherModel(climate, 42);
        assertEquals(a.day(700), late.day(700));
        assertNotEquals(a.day(10).state(), null);
        // May 1 of year 1 is Clear with no anomaly.
        assertEquals(DayState.CLEAR, a.day(0).state());
        assertEquals(0, a.day(0).anomalyC());
    }

    /** Coldest just before sunrise and warmest at 15:00; winds average the month's normal; hours stay near bounds. */
    @Test
    void hoursAndWind() {
        WeatherModel w = new WeatherModel(climate, 9);
        for (long d = 0; d < 365; d++) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            int at = -1, low = -1;
            for (int h = 0; h < 24; h++) {
                double t = w.hourAirC(d, h);
                if (t > max) { max = t; at = h; }
                if (t < min) { min = t; low = h; }
            }
            if (max - min > 0.5) {
                assertEquals(15, at, "warmest hour, day " + d);
                assertEquals(5, low, "coldest hour, day " + d);
            }
        }
        double[] wind = new double[12];
        int[] n = new int[12];
        for (long d = 0; d < YEARS * 365L; d++) {
            int m = Calendar.month(Calendar.dayOfYearOfDay(d));
            for (int h = 0; h < 24; h += 2) wind[m - 1] += w.hour(d * 1440 + h * 60).windMs() / 12;
            n[m - 1]++;
        }
        for (int m = 1; m <= 12; m++) assertEquals(climate.windMs()[m - 1], wind[m - 1] / n[m - 1], 0.35, "wind, month " + m);
    }

    /** Water follows the 5-day mean air: creeks 2–4 °C or colder early in May, 8–16 °C in July. */
    @Test
    void water() {
        WeatherModel w = new WeatherModel(climate, 5);
        double may = 0, july = 0;
        for (int y = 0; y < YEARS; y++) {
            may += w.creekC(y * 365L + 2);
            july += w.creekC(y * 365L + Calendar.dayOfYear(7, 20));
            assertTrue(w.creekC(y * 365L + Calendar.dayOfYear(1, 15)) >= climate.waterFloorC());
        }
        may /= YEARS;
        july /= YEARS;
        assertTrue(may >= 0.5 && may <= 4.5, "early May creek " + may);
        assertTrue(july >= 8 && july <= 16, "July creek " + july);
        assertTrue(w.lakeC(Calendar.dayOfYear(7, 20)) > w.creekC(Calendar.dayOfYear(7, 20)));
    }

    @Test
    void placeRules() {
        WeatherModel w = new WeatherModel(climate, 1);
        assertEquals(10 - 0.65, w.airAtHeight(10, climate.floorY() + 100, false), 1e-9);
        assertEquals(-30 + 3, w.airAtHeight(-30, climate.floorY() + 100, true), 1e-9);
        assertEquals(-30 + 8 - 0.65 * (500 - 800.0 / 3) / 100, w.airAtHeight(-30, climate.floorY() + 500, true), 1e-9);
        assertEquals(4 * 1.6, w.windAtPlace(4, climate.floorY() + 450, false), 1e-9);
        assertEquals(4 * 0.3, w.windAtPlace(4, climate.floorY(), true), 1e-9);
        assertEquals(42, w.snowDepthCm(Calendar.dayOfYear(12, 31)), 1e-9);
        assertEquals(0, w.snowDepthCm(Calendar.dayOfYear(7, 10)), 1e-9);
    }
}
