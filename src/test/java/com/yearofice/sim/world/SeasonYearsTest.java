package com.yearofice.sim.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.Climate;
import com.yearofice.sim.weather.ClimateJson;
import com.yearofice.sim.weather.WeatherModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SnowPack.SnowClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Three simulated years, back to back, on the valley's drawn weather (M7: river ice forms 20 Oct - 15 Nov every year). */
class SeasonYearsTest {
    static Climate climate;

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/yearofice/weather/interior_alaska.json"), StandardCharsets.UTF_8);
        climate = ClimateJson.parse(JsonParser.parseString(json).getAsJsonObject()).climate();
    }

    static final int OCT_20 = 172, NOV_15 = 198;

    @Test
    void riverIceFormsLateOctoberToMidNovemberEveryYear() {
        for (long seed : new long[] {1, 77, 3141592, 42, 2026, 7919, 15838, 23757, 31676, 39595}) {
            WeatherModel w = new WeatherModel(climate, seed);
            SeasonCore core = new SeasonCore();
            SeasonPreset p = SeasonPreset.STANDARD;
            int[] freeze = {-1, -1, -1};
            double[] peak = new double[3];
            for (long d = 0; d < 365 * 3; d++) {
                WeatherModel.Day day = w.day(d);
                int doy = Calendar.dayOfYearOfDay(d);
                core.step(d, doy, day.meanC(), day.precipMm(), day.windMs(), p.snow, p.ice);
                int y = (int) (d / 365);
                if (core.lastEvent[BodyClass.RIVER.ordinal()] == IceModel.Event.FREEZE_UP && freeze[y] < 0 && doy > 100) freeze[y] = doy;
                peak[y] = Math.max(peak[y], core.snow(SnowClass.OPEN).depthCm());
            }
            for (int y = 0; y < 3; y++) {
                assertTrue(freeze[y] >= OCT_20 && freeze[y] <= NOV_15, "seed " + seed + " year " + y + " froze on doy " + freeze[y]);
                assertTrue(peak[y] > 15 && peak[y] < 110, "seed " + seed + " peak snow " + peak[y]);
            }
        }
    }

    @Test
    void iceBreaksUpAndFloodReturns() {
        WeatherModel w = new WeatherModel(climate, 77);
        SeasonCore core = new SeasonCore();
        SeasonPreset p = SeasonPreset.STANDARD;
        boolean breakup = false;
        double maxThick = 0, maxFlood = 0;
        long floodEnd = -1, breakupDay = -1;
        for (long d = 0; d < 395; d++) {
            WeatherModel.Day day = w.day(d);
            core.step(d, Calendar.dayOfYearOfDay(d), day.meanC(), day.precipMm(), day.windMs(), p.snow, p.ice);
            maxThick = Math.max(maxThick, core.ice(BodyClass.LAKE).cm);
            if (core.lastEvent[0] == IceModel.Event.BREAKUP) { breakup = true; breakupDay = d; }
            maxFlood = Math.max(maxFlood, core.floodM);
            if (breakup && core.floodM == 0 && floodEnd < 0) floodEnd = d;
        }
        System.out.println("lake max " + maxThick + " breakup day " + breakupDay + " flood end " + floodEnd + " maxFlood " + maxFlood);
        assertTrue(breakup, "river ice broke up");
        assertTrue(maxThick > 60 && maxThick < 200, "lake ice " + maxThick);
        assertTrue(floodEnd > breakupDay && floodEnd - breakupDay < 30, "flood returned");
        assertEquals(0, core.floodM, 0);
    }

    @Test
    void holeSkinsInAnHourBelowMinus20() {
        IceTuning t = SeasonPreset.STANDARD.ice;
        assertTrue(IceModel.holeSkinMinutes(-21, t) <= 60);
        assertTrue(IceModel.holeSkinMinutes(-10, t) > 60);
        assertTrue(Double.isInfinite(IceModel.holeSkinMinutes(-3, t)));
    }
}
