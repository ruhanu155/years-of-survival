package com.yearofice.sim.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Blueprint › Warmth › Fire, Crafting › Warm rooms. */
class HeatTest {

    @Test
    void openFireKeepsItsBonusTo3mAndFadesInALineTo8m() {
        assertEquals(25, HeatIndex.contribution(HeatSource.openFire(FireSetting.HOT, 0.5)), 1e-12);
        assertEquals(25, HeatIndex.contribution(HeatSource.openFire(FireSetting.HOT, 3)), 1e-12);
        assertEquals(12.5, HeatIndex.contribution(HeatSource.openFire(FireSetting.HOT, 5.5)), 1e-12);
        assertEquals(0, HeatIndex.contribution(HeatSource.openFire(FireSetting.HOT, 8)), 1e-12);
        assertEquals(10, HeatIndex.contribution(HeatSource.openFire(FireSetting.LOW, 2)), 1e-12);
        assertEquals(18, HeatIndex.contribution(HeatSource.openFire(FireSetting.STEADY, 2)), 1e-12);
    }

    @Test
    void indoorHeatersFadeTo0At3m() {
        assertEquals(10, HeatIndex.contribution(HeatSource.stove(1.5)), 1e-12);
        assertEquals(5, HeatIndex.contribution(HeatSource.stove(2.25)), 1e-12);
        assertEquals(15, HeatIndex.contribution(HeatSource.fireplace(2)), 1e-12);
        assertEquals(0, HeatIndex.contribution(HeatSource.fireplace(3)), 1e-12);
    }

    @Test
    void stackedSourcesNeverExceedTheHottestOne() {
        List<HeatSource> two = List.of(HeatSource.openFire(FireSetting.HOT, 1), HeatSource.openFire(FireSetting.HOT, 2));
        assertEquals(25, HeatIndex.bonusC(two), 1e-12);
        List<HeatSource> far = List.of(HeatSource.openFire(FireSetting.HOT, 6.5), HeatSource.openFire(FireSetting.HOT, 6.5));
        assertEquals(15, HeatIndex.bonusC(far), 1e-12);
        assertEquals(0, HeatIndex.bonusC(List.of()), 1e-12);
    }

    @Test
    void eachBlockBetweenDividesTheEffect() {
        assertEquals(9, HeatIndex.contribution(HeatSource.openFire(FireSetting.STEADY, 1).behind(1)), 1e-12);
        assertEquals(6, HeatIndex.contribution(HeatSource.openFire(FireSetting.STEADY, 1).behind(2)), 1e-12);
    }

    @Test
    void within3mOfAnUnoccludedFire() {
        assertTrue(HeatIndex.withinFire(List.of(HeatSource.openFire(FireSetting.LOW, 3)), 3));
        assertFalse(HeatIndex.withinFire(List.of(HeatSource.openFire(FireSetting.LOW, 3.5)), 3));
        assertFalse(HeatIndex.withinFire(List.of(HeatSource.openFire(FireSetting.LOW, 1).behind(1)), 3));
    }

    // Crafting › Warm rooms: the first cabin, fully chinked, sod roof, plank floor, barrel stove.
    private static final RoomHeat.Room CHINKED = new RoomHeat.Room(27, 5, 356);
    private static final RoomHeat.Room UNCHINKED = new RoomHeat.Room(71, 5, 356);

    /** Rise above outside over the last simulated day: {average, minimum}. */
    private static double[] simulate(RoomHeat.Room room, int armloadsPerDay, double outC) {
        RoomHeat.Heater stove = RoomHeat.Heater.BARREL_STOVE;
        double burnMin = stove.burnHours(FireSetting.STEADY, 1.0) * 60;
        double power = stove.powerW(FireSetting.STEADY);
        double ground = RoomHeat.groundC(outC);
        double t = outC, sum = 0, min = Double.MAX_VALUE;
        int days = 20, spacing = 1440 / armloadsPerDay;
        for (int m = 0; m < days * 1440; m++) {
            int inDay = m % 1440;
            boolean burning = inDay % spacing < burnMin && inDay / spacing < armloadsPerDay;
            t = RoomHeat.stepMinute(room, t, outC, ground, burning ? power : 0);
            if (m >= (days - 1) * 1440) {
                sum += t - outC;
                min = Math.min(min, t - outC);
            }
        }
        return new double[] {sum / 1440, min};
    }

    @Test
    void theFirstCabinHasAnElevenHourTimeConstant() {
        assertEquals(11.1, CHINKED.tauHours(), 0.1);
    }

    @Test
    void roomTestChinkedCabinOnTwoArmloadsAveragesPlus26ToPlus32AndNeverDropsBelowPlus18() {
        double[] r = simulate(CHINKED, 2, -30);
        assertTrue(r[0] >= 26 && r[0] <= 32, "average rise " + r[0]);
        assertTrue(r[1] >= 18, "minimum rise " + r[1]);
    }

    @Test
    void draftTestTheUnchinkedCabinNeedsFourToSixArmloadsToMatch() {
        int needed = -1;
        for (int a = 1; a <= 8; a++) {
            if (simulate(UNCHINKED, a, -30)[0] >= 26) {
                needed = a;
                break;
            }
        }
        assertTrue(needed >= 4 && needed <= 6, "armloads " + needed);
    }

    @Test
    void closedFormSettleMatchesSteppingOffScreen() {
        double stepped = 5;
        for (int m = 0; m < 600; m++) stepped = RoomHeat.stepMinute(CHINKED, stepped, -30, -5, 2500);
        double settled = RoomHeat.settle(CHINKED, 5, -30, -5, 2500, 10);
        assertEquals(settled, stepped, 0.05);
    }

    @Test
    void groundUnderACabinIsHeldBetweenMinus5And8() {
        assertEquals(-5, RoomHeat.groundC(-30), 1e-12);
        assertEquals(8, RoomHeat.groundC(15), 1e-12);
        assertEquals(2, RoomHeat.groundC(2), 1e-12);
    }

    @Test
    void lAirAddsSeamsDoorsAndTheBaseLeak() {
        assertEquals(10 + 0.4 * 10 + 0.1 * 20 + 40 + 1.5, RoomHeat.lAir(10, 10, 20, 1), 1e-12);
    }

    @Test
    void heaterArmloadsMatchTheTable() {
        RoomHeat.Heater b = RoomHeat.Heater.BARREL_STOVE;
        assertEquals(RoomHeat.ARMLOAD_KWH * b.keptShare,
                b.powerW(FireSetting.STEADY) * b.burnHours(FireSetting.STEADY, 1) / 1000, 0.05);
        assertEquals(14, b.burnHours(FireSetting.LOW, 1.4), 1e-9); // "4 × 1.4 ÷ 0.4" via the Low row
    }
}
