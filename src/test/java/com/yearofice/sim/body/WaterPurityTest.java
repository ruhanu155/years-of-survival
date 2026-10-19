package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import org.junit.jupiter.api.Test;

/** Fire, water and light › Water, Water quality, boiling; Realism presets › unboiled water illness chance. */
class WaterPurityTest {

    @Test
    void unboiledChanceByPreset() {
        assertEquals(0.15, WaterPurity.illnessChance(WaterPurity.Source.RIVER, false, BodyPreset.GENTLE), 1e-12);
        assertEquals(0.30, WaterPurity.illnessChance(WaterPurity.Source.RIVER, false, BodyPreset.STANDARD), 1e-12);
        assertEquals(0.40, WaterPurity.illnessChance(WaterPurity.Source.CREEK, false, BodyPreset.BRUTAL), 1e-12);
    }

    @Test
    void beaverPondsAndWasteDoubleIt() {
        assertEquals(0.60, WaterPurity.illnessChance(WaterPurity.Source.BEAVER_POND, false, BodyPreset.STANDARD), 1e-12);
        assertEquals(0.80, WaterPurity.illnessChance(WaterPurity.Source.DOWNSTREAM_OF_WASTE, false, BodyPreset.BRUTAL), 1e-12);
        assertEquals(1.0, WaterPurity.illnessChance(WaterPurity.Purity.FOUL, false, 0.5 * 1.5), 1e-12);
    }

    @Test
    void safeSourcesAndBoiledWaterNeverSicken() {
        for (WaterPurity.Source s : WaterPurity.Source.values()) {
            assertEquals(0, WaterPurity.illnessChance(s, true, BodyPreset.BRUTAL), 1e-12, s.name());
        }
        assertEquals(0, WaterPurity.illnessChance(WaterPurity.Source.RAIN, false, BodyPreset.BRUTAL), 1e-12);
        assertEquals(0, WaterPurity.illnessChance(WaterPurity.Source.MELTED_SNOW, false, BodyPreset.BRUTAL), 1e-12);
        assertEquals(0, WaterPurity.illnessChance(WaterPurity.Source.BIRCH_SAP, false, BodyPreset.BRUTAL), 1e-12);
        assertEquals(0, WaterPurity.illnessChance(WaterPurity.Source.HOT_SPRING, false, BodyPreset.BRUTAL), 1e-12);
    }

    @Test
    void tenThousandRiverDrinksOnStandardSickenAboutThirtyPercentOneToThreeDaysLater() {
        Mulberry32 rng = new Mulberry32(20261019);
        int ill = 0;
        for (int i = 0; i < 10_000; i++) {
            WaterPurity.Drink d = WaterPurity.drink(WaterPurity.Purity.RAW, false, BodyPreset.STANDARD.unboiledIllness, rng);
            if (d.ill()) {
                ill++;
                assertTrue(d.onsetMinutes() >= 1440 && d.onsetMinutes() <= 4320, "onset " + d.onsetMinutes());
            } else {
                assertEquals(-1, d.onsetMinutes());
            }
        }
        assertTrue(ill >= 2800 && ill <= 3200, "ill " + ill);
    }

    @Test
    void drinksReplayFromTheSameSeed() {
        Mulberry32 a = new Mulberry32(7), b = new Mulberry32(7);
        for (int i = 0; i < 100; i++) {
            assertEquals(WaterPurity.drink(WaterPurity.Purity.FOUL, false, 0.3, a),
                    WaterPurity.drink(WaterPurity.Purity.FOUL, false, 0.3, b));
        }
    }

    @Test
    void boilingTimes() {
        assertEquals(25, WaterPurity.minutesToSafe(WaterPurity.Heat.STEADY_FIRE, 1), 1e-12);
        assertEquals(20 * 1.6 + 5, WaterPurity.minutesToSafe(WaterPurity.Heat.STEADY_FIRE, 2), 1e-12);
        assertEquals(30, WaterPurity.minutesToBoil(WaterPurity.Heat.SMALL_FIRE, 1), 1e-12);
        assertEquals(12, WaterPurity.minutesToBoil(WaterPurity.Heat.TEEPEE, 1), 1e-12);
        assertEquals(15, WaterPurity.hotStonesMinutesToSafe(), 1e-12);
        assertFalse(WaterPurity.safeAfterRollingBoil(4.9));
        assertTrue(WaterPurity.safeAfterRollingBoil(5));
    }
}
