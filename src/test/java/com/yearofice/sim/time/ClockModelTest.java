package com.yearofice.sim.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.sim.weather.ClimateJson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Layer 1's pure model: rates, the arbiter, the calendar, the daylight table and the vanilla time map. */
class ClockModelTest {
    static Sun sun;
    static double[] daylightNormals;

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/yearofice/weather/interior_alaska.json"), StandardCharsets.UTF_8);
        ClimateJson.Parsed p = ClimateJson.parse(JsonParser.parseString(json).getAsJsonObject());
        sun = p.sun();
        daylightNormals = p.climate().daylightH();
    }

    /**
     * One real minute (1,200 ticks) at each rate adds exactly 1,200 × its ds; World (and Day 1, half of it) is a
     * third on the 72-minute day.
     */
    @Test
    void ratesExact() {
        Rates r = Rates.STANDARD;
        int[] want = {3 * 1200, 150 * 1200, 600 * 1200, 1800 * 1200, 6 * 1200, 15 * 1200, 30 * 1200};
        for (Rate rate : Rate.values()) {
            long ds = 0;
            for (int t = 0; t < 1200; t++) ds += r.dsPerTick(rate, 24);
            assertEquals(want[rate.ordinal()], ds, rate.id());
            long ds72 = 0;
            for (int t = 0; t < 1200; t++) ds72 += r.dsPerTick(rate, 72);
            assertEquals(rate == Rate.WORLD || rate == Rate.DAY_ONE ? want[rate.ordinal()] / 3 : want[rate.ordinal()], ds72, rate.id() + " at 72 min");
        }
        // Every allowed day length divides exactly, and a whole game day takes exactly L real minutes at World.
        for (int l : r.dayLengths()) assertEquals(Calendar.DS_PER_DAY, (long) r.dsPerTick(Rate.WORLD, l) * l * 60 * 20, "L=" + l);
        assertThrows(IllegalArgumentException.class, () -> new Rates(3, 6, 30, 150, 600, 1800, new int[] {25}));
        assertEquals(36, r.nearestAllowed(35));
        assertTrue(r.allowed(45) && !r.allowed(50));
    }

    /** {@code clock/rates.json} holds the doc's rates (Layer 1's rate table, RECONCILE 117 and 145). */
    @Test
    void ratesFileIsTheDocs() throws Exception {
        var o = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/yearofice/clock/rates.json"))).getAsJsonObject();
        var d = o.getAsJsonObject("ds_per_tick");
        var l = o.getAsJsonArray("allowed_lengths_min");
        int[] lens = new int[l.size()];
        for (int i = 0; i < lens.length; i++) lens[i] = l.get(i).getAsInt();
        assertEquals(Rates.STANDARD, new Rates(d.get("close").getAsInt(), d.get("travel").getAsInt(), d.get("world_at_l24").getAsInt(),
                d.get("work").getAsInt(), d.get("until_done").getAsInt(), d.get("sleep").getAsInt(), lens));
        assertEquals(Rates.DEFAULT_DAY_ONE_PCT, o.get("first_fire_world_pct").getAsInt(), "Day 1 is half speed");
    }

    @Test
    void arbiter() {
        assertEquals(Rate.WORLD, RateArbiter.forPlayer(EnumSet.noneOf(Rate.class)));
        assertEquals(Rate.CLOSE, RateArbiter.forPlayer(EnumSet.of(Rate.WORK, Rate.CLOSE, Rate.TRAVEL)));
        assertEquals(Rate.WORK, RateArbiter.forPlayer(EnumSet.of(Rate.WORK, Rate.TRAVEL)));
        assertEquals(Rate.SLEEP, RateArbiter.forPlayer(EnumSet.of(Rate.SLEEP, Rate.TRAVEL)));
        // clock/multiplayer_slowest: a walker and a worker give the travel rate.
        assertEquals(Rate.TRAVEL, RateArbiter.forWorld(List.of(Rate.TRAVEL, Rate.WORK), Rates.STANDARD, 24));
        // Sleep skips only when every player sleeps.
        assertEquals(Rate.WORLD, RateArbiter.forWorld(List.of(Rate.SLEEP, Rate.WORLD), Rates.STANDARD, 72));
        assertEquals(Rate.SLEEP, RateArbiter.forWorld(List.of(Rate.SLEEP, Rate.SLEEP), Rates.STANDARD, 24));
        assertEquals(Rate.WORLD, RateArbiter.forWorld(List.of(), Rates.STANDARD, 24));
    }

    @Test
    void calendar() {
        assertEquals("May 1, year 1, 00:00", Calendar.format(0));
        long apr30 = 364 * Calendar.DS_PER_DAY + (23 * 60 + 59) * Calendar.DS_PER_MINUTE;
        assertEquals("April 30, year 1, 23:59", Calendar.format(apr30));
        assertEquals("May 1, year 2, 00:00", Calendar.format(Calendar.DS_PER_YEAR));
        assertEquals(5, Calendar.month(0));
        assertEquals(6, Calendar.month(31));
        assertEquals(2, Calendar.month(Calendar.dayOfYear(2, 28)));
        assertEquals(3, Calendar.month(Calendar.dayOfYear(2, 28) + 1));
        assertEquals(51, Calendar.dayOfYear(6, 21));
        for (int d = 0; d < 365; d++) assertEquals(d, Calendar.dayOfYear(Calendar.month(d), Calendar.dayOfMonth(d)));
        assertEquals(-1, Calendar.absDay(-1));
        assertEquals(364, Calendar.dayOfYear(-1));
    }

    /** The table's mid-month daylight matches the climate table within half an hour; the solstices read right. */
    @Test
    void daylight() {
        int[] months = {5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4};
        for (int m : months) {
            int d = Calendar.dayOfYear(m, 15);
            assertEquals(daylightNormals[m - 1], sun.daylightHours(d), 0.5, "month " + m);
        }
        int jun21 = Calendar.dayOfYear(6, 21), dec21 = Calendar.dayOfYear(12, 21);
        assertTrue(sun.daylightHours(jun21) > 21 && sun.daylightHours(dec21) < 4);
        assertTrue(sun.elevation(jun21, 14 * 60) > 45, "high June sun");
        assertTrue(sun.elevation(dec21, 14 * 60) < 3, "low December sun");
        // Noon is south (180° ± 25°), the June midnight sun north.
        assertEquals(180, sun.azimuth(jun21, 13 * 60 + 50), 25);
        double north = sun.azimuth(jun21, 60 + 50);
        assertTrue(north < 30 || north > 330, "June 01:50 sun is north: " + north);
    }

    /** clock/dawn_fallback_june, in model form: June 21 has one dawn and one dusk, by the 120-minute fallback. */
    @Test
    void dawnAndDusk() {
        int jun21 = Calendar.dayOfYear(6, 21);
        assertTrue(sun.dawnIsFallback(jun21) && sun.duskIsFallback(jun21));
        int dawn = sun.dawnMinute(jun21), dusk = sun.duskMinute(jun21);
        assertTrue(dawn > 0 && dawn < 8 * 60, "dawn " + dawn);
        assertTrue(dusk > 20 * 60 && dusk < 26 * 60, "dusk " + dusk);
        // An ordinary day: dawn before sunrise before sunset before dusk, the −6° crossings real.
        int sep21 = Calendar.dayOfYear(9, 21);
        assertFalse(sun.dawnIsFallback(sep21) || sun.duskIsFallback(sep21));
        assertTrue(sun.dawnMinute(sep21) < sun.sunriseMinute(sep21) && sun.sunriseMinute(sep21) < sun.sunsetMinute(sep21)
                && sun.sunsetMinute(sep21) < sun.duskMinute(sep21));
        assertEquals(Sun.TWILIGHT_DEG, sun.elevation(sep21, sun.dawnMinute(sep21)), 0.5);
        // Every day has a dawn and a dusk, in order, within the day's window.
        for (int d = 0; d < 365; d++) {
            assertTrue(sun.dawnMinute(d) < 720 && sun.duskMinute(d) > 720 && sun.duskMinute(d) < 2160, "day " + d);
            assertTrue(sun.duskMinute(d) - 1440 < sun.dawnMinute(d + 1), "dusk before the next dawn, day " + d);
        }
    }

    /** The 181-entry map gives back the elevation on vanilla's sun, on the right half of the sky. */
    @Test
    void vanillaTimeMap() {
        for (double e = -90; e <= 90; e += 0.25) {
            long up = VanillaTime.dayTime(e, true), down = VanillaTime.dayTime(e, false);
            assertEquals(e, VanillaTime.vanillaElevation(up), 0.05, "rising " + e);
            assertEquals(e, VanillaTime.vanillaElevation(down), 0.05, "setting " + e);
            if (e > -89 && e < 89) {
                assertTrue(down > 6_000 && down < 18_000, "afternoon half " + down);
                assertTrue(up > 18_000 || up < 6_000, "morning half " + up);
            }
        }
        // Vanilla's eased sun stands 12° up at day time 0 and noon is 6,000.
        assertEquals(6_000, VanillaTime.dayTime(90, false), 2, "noon");
        assertEquals(18_000, VanillaTime.dayTime(-90, false), 2, "midnight");
    }

    @Test
    void moon() {
        assertEquals(0, Moon.phase(0, 0), 1e-12);
        assertEquals(0.5, Moon.phase(Math.round(Moon.SYNODIC_DAYS / 2 * 1440), 0), 1e-3);
        assertEquals(Moon.phase(1000, 3), Moon.phase(1000 + Math.round(Moon.SYNODIC_DAYS * 1440 * 100), 3), 1e-3);
    }
}
