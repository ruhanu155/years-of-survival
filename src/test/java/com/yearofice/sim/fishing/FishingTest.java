package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.fishing.BiteModel.Conditions;
import com.yearofice.sim.fishing.BiteModel.TimeBand;
import com.yearofice.sim.fishing.BiteModel.Weather;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The bite model, the depth size draw, stocks and tackle (Blueprint › Fishing in depth). */
class FishingTest {
    static Conditions cond(TimeBand t, Weather w, boolean falling) {
        return new Conditions(t, true, 2.5, w, falling, 1.0, 10, 0.1, false, false, 0, false,
                0.6, 30, 30, 1, 5, "fly", false);
    }

    @Test
    void sizeDrawPikeMeanAndRange() {
        FishSpecies pike = FishSpecies.northernPike();
        Mulberry32 r = new Mulberry32(11);
        double sum = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            double kg = SizeDraw.kg(pike, 1.0, 1.0, 0.75, r.nextDouble(), r.nextDouble(), r.nextDouble());
            boolean trophy = kg > pike.kgMax();
            assertTrue(trophy ? kg >= 10 && kg <= 15 : kg >= pike.kgMin() && kg <= pike.kgMax(), "kg " + kg);
            sum += kg;
        }
        assertEquals(4.0, sum / n, 0.2);
        assertEquals(4.0, SizeDraw.meanKg(pike, 1.0, 1.0), 1e-9);
    }

    @Test
    void deeperWaterDrawsBiggerFish() {
        FishSpecies pike = FishSpecies.northernPike();
        assertEquals(1.6, SizeDraw.exponent(2.5), 0);
        assertEquals(2.0, SizeDraw.exponent(2.0), 0);
        assertEquals(1.3, SizeDraw.exponent(5.5), 0);
        Mulberry32 r = new Mulberry32(12);
        double deep = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) deep += SizeDraw.kg(pike, 3.0, 1.0, 0.5, r.nextDouble(), 1, 0);
        assertEquals(4.5, deep / n, 0.15); // "about 4.5 kg on a deep drop-off"
        assertTrue(SizeDraw.meanKg(pike, 6, 1) > SizeDraw.meanKg(pike, 3, 1));
        assertTrue(SizeDraw.meanKg(pike, 1, 0.2) < SizeDraw.meanKg(pike, 1, 1)); // a fished-down spot
    }

    @Test
    void biteFactors() {
        assertEquals(1.0, BiteModel.mPlace(4, 5), 0);
        assertEquals(0.3, BiteModel.mPlace(9, 5), 0);
        assertEquals(0.05, BiteModel.mPlace(11, 5), 0);
        assertEquals(0.5, BiteModel.mStock(25, 100), 1e-12);
        assertEquals(0.1, BiteModel.clampProduct(0.01), 0);
        assertEquals(3.0, BiteModel.clampProduct(9), 0);
        FishSpecies burbot = FishSpecies.burbot();
        FishSpecies grayling = FishSpecies.arcticGrayling();
        Conditions night = cond(TimeBand.NIGHT, Weather.CLEAR, false);
        assertEquals(2.0, BiteModel.mTime(burbot, night), 0);
        assertEquals(0.5, BiteModel.mTime(grayling, night), 0);
        assertEquals(0.4, BiteModel.mTurbid(grayling, new Conditions(TimeBand.DAY, true, 1, Weather.RAIN, false, 1, 10, 1.0,
                false, false, 0, false, 0.5, 1, 1, 0, 5, "fly", false)), 1e-12);
        double p = BiteModel.pBite(grayling, cond(TimeBand.DAY, Weather.CLEAR, false));
        double spooked = BiteModel.pBite(grayling, new Conditions(TimeBand.DAY, true, 2.5, Weather.CLEAR, false, 1.0, 10, 0.1,
                false, false, 0, false, 0.6, 30, 30, 1, 5, "fly", true));
        assertEquals(p / 2, spooked, 1e-12);
    }

    /** Fishing in depth › Conditions test: falling-pressure afternoons bite 1.4 ±0.05 times steady ones. */
    @Test
    void fallingPressureAfternoons() {
        FishSpecies grayling = FishSpecies.arcticGrayling();
        Mulberry32 r = new Mulberry32(3);
        double falling = 0, steady = 0;
        for (int day = 0; day < 1000; day++) {
            double temp = 6 + 10 * r.nextDouble();
            double turb = 0.5 * r.nextDouble();
            Conditions f = new Conditions(TimeBand.DAY, true, 2.5, Weather.CLEAR, true, 1, temp, turb, false, false, 0,
                    false, 0.6, 30, 30, 1, 5, "fly", false);
            Conditions s = new Conditions(TimeBand.DAY, true, 2.5, Weather.CLEAR, false, 1, temp, turb, false, false, 0,
                    false, 0.6, 30, 30, 1, 5, "fly", false);
            falling += BiteModel.pBite(grayling, f);
            steady += BiteModel.pBite(grayling, s);
        }
        assertEquals(1.4, falling / steady, 0.05);
    }

    @Test
    void rollMinuteFirstSuccessWins() {
        List<FishSpecies> sp = List.of(FishSpecies.arcticGrayling(), FishSpecies.humpbackWhitefish());
        Conditions c = cond(TimeBand.DAWN_DUSK, Weather.CLOUDY, false);
        assertEquals(0, BiteModel.rollMinute(sp, List.of(c, c), new double[] {0, 0}));
        assertEquals(1, BiteModel.rollMinute(sp, List.of(c, c), new double[] {0.99, 0}));
        assertEquals(-1, BiteModel.rollMinute(sp, List.of(c, c), new double[] {0.99, 0.99}));
    }

    @Test
    void stocksRegrowAndEcho() {
        FishStock s = new FishStock(18, 60);
        for (int m = 6; m <= 8; m++) s = s.regrow(0.5, FishStock.IMMIGRATION_RIVER, m);
        assertTrue(s.fraction() > 0.79, "3 months: " + s.fraction());
        s = s.regrow(0.5, FishStock.IMMIGRATION_RIVER, 9);
        assertTrue(s.fraction() > 0.8);
        FishStock winter = new FishStock(18, 60).regrow(0.5, 0, 1);
        assertEquals(18 + 0.125 * 18 * 0.7, winter.n(), 1e-9);
        assertEquals(0.2 * 30, FishStock.nextSpawnCapacity(30, 15), 1e-9);
        assertEquals(1.0, FishStock.echo(0.2), 0);
        assertEquals(0.85, FishStock.echo(0.3), 1e-12);
        assertEquals(0.4, FishStock.echo(0.9), 0);
        assertEquals(new FishStock(10, 10), new FishStock(30, 60).rescale(10));
    }

    /** hunt/line_strength: a 4 kg line holds 4.0 dry and new, 3.0 wet, 2.1 wet at condition 0. */
    @Test
    void line_strength() {
        Tackle t = Tackle.plantFiberWillow();
        assertEquals(4.0, t.effectiveStrengthKg(), 1e-12);
        assertEquals(3.0, t.withWet(true).effectiveStrengthKg(), 1e-12);
        assertEquals(2.1, t.withWet(true).withCondition(0).effectiveStrengthKg(), 1e-12);
    }

    @Test
    void setLineChance() {
        assertEquals(0.5, PassiveGear.setLineHourly(0.1), 0);
        assertEquals(0.03, PassiveGear.setLineHourly(0.001), 1e-12);
        assertEquals(0.0, Thresholds.stream(1, "x").nextDouble() * 0, 0);
    }
}
