package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.env.EnvSample;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Clothing › Safe-to temperatures and Health › Frostbite on the part counters. */
class FrostTest {

    private static int pointsAfter(int minutes, double limit, double feel) {
        int p = 0;
        for (int i = 0; i < minutes; i++) p = FrostModel.stepMinute(p, limit, feel, false, true).points();
        return p;
    }

    @Test
    void furMittensAreSafeAtMinus25ButNotMinus26() {
        ClothingState hands = ClothingState.of(Wardrobe.WORK_GLOVES, Wardrobe.FUR_MITTENS);
        double limit = Clothing.zoneSafeToC(hands, Zone.HANDS);
        assertEquals(-25, limit, 1e-12);

        double at25 = Warmth.zoneExposureC(EnvSample.outdoors(-25, 0), hands, List.of(), WarmthContext.RESTING, Zone.HANDS);
        double at26 = Warmth.zoneExposureC(EnvSample.outdoors(-26, 0), hands, List.of(), WarmthContext.RESTING, Zone.HANDS);
        assertEquals(0, pointsAfter(24 * 60, limit, at25), "−25 °C is safe");
        assertTrue(pointsAfter(1, limit, at26) > 0, "−26 °C is not");
        assertEquals(300, FrostModel.tNipMinutes(limit, at26), 1e-9);
        assertEquals(0, FrostModel.stage(pointsAfter(299, limit, at26)));
        assertEquals(1, FrostModel.stage(pointsAfter(300, limit, at26)));
    }

    @Test
    void safeToLimitsByGear() {
        assertEquals(-20, Clothing.zoneSafeToC(ClothingState.naked(), Zone.HANDS), 1e-12);
        assertEquals(-22, Clothing.zoneSafeToC(ClothingState.of(Wardrobe.WORK_GLOVES), Zone.HANDS), 1e-12);
        assertEquals(-22, Clothing.zoneSafeToC(ClothingState.of(Wardrobe.WOOL_SOCKS, Wardrobe.HIKING_BOOTS), Zone.FEET), 1e-12);
        assertEquals(-35, Clothing.zoneSafeToC(ClothingState.of(Wardrobe.HARE_LINERS, Wardrobe.MUKLUKS), Zone.FEET), 1e-12);
        assertEquals(-30, Clothing.zoneSafeToC(ClothingState.of(Wardrobe.FUR_HAT), Zone.HEAD), 1e-12);
    }

    @Test
    void aWetZoneUsesTheWetLimitWhateverIsWorn() {
        ClothingState c = ClothingState.of(Wardrobe.HARE_LINERS, Wardrobe.MUKLUKS)
                .withWet(Layer.slot(Zone.FEET, Layer.INNER), 0.8);
        assertEquals(-10, Clothing.zoneSafeToC(c, Zone.FEET), 1e-12);
        assertEquals(-10, FrostModel.partLimitC(-25, true, false), 1e-12);
        assertEquals(-20, FrostModel.partLimitC(-25, false, true), 1e-12);
    }

    @Test
    void stagesFollowEachFurtherPeriodAndTenPastTheLimitGivesThirtyMinutes() {
        assertEquals(30, FrostModel.tNipMinutes(-20, -30), 1e-12);
        assertEquals(10, FrostModel.tNipMinutes(-20, -50), 1e-12);
        assertEquals(1, FrostModel.stage(pointsAfter(30, -20, -30)));
        assertEquals(2, FrostModel.stage(pointsAfter(60, -20, -30)));
        assertEquals(3, FrostModel.stage(pointsAfter(90, -20, -30)));
    }

    @Test
    void rewarmingStopsFrostAndClearsFrostnipWithinTheHour() {
        int p = pointsAfter(30, -20, -30);
        assertEquals(1, FrostModel.stage(p));
        for (int i = 0; i < 60; i++) p = FrostModel.stepMinute(p, -20, -30, true, true).points();
        assertEquals(0, p);
    }

    @Test
    void deepFrostbiteUnrewarmedSixHoursTakesADigitOnce() {
        int p = FrostModel.DEEP;
        int lost = 0;
        for (int i = 0; i < 400; i++) {
            FrostModel.PartStep s = FrostModel.stepMinute(p, -20, -30, false, true);
            if (s.digitLost()) {
                lost++;
                assertEquals(FrostModel.DEEP_DIGIT_MINUTES - 1, i);
            }
            p = s.points();
        }
        assertEquals(1, lost);
        assertFalse(FrostModel.stepMinute(FrostModel.DEEP + 359, -20, -30, false, false).digitLost(), "Gentle keeps digits");
    }

    @Test
    void aZonesFrostIsItsWorstPart() {
        int[] f = new int[Part.COUNT];
        f[Part.LEFT_HAND.ordinal()] = 12;
        f[Part.RIGHT_HAND.ordinal()] = 40;
        assertEquals(40, FrostModel.zoneValue(f, Zone.HANDS));
        assertEquals(0, FrostModel.zoneValue(f, Zone.FEET));
    }
}
