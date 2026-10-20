package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import org.junit.jupiter.api.Test;

/** Clothing › Wet, with B13 glove grease. */
class ClothingTest {
    private static final int HANDS_IN = Layer.slot(Zone.HANDS, Layer.INNER);
    private static final int TORSO_IN = Layer.slot(Zone.TORSO, Layer.INNER);

    @Test
    void glove_wetness() {
        ClothingState c = ClothingState.of(Wardrobe.FLEECE, Wardrobe.WORK_GLOVES);
        EnvSample rain = EnvSample.outdoors(8, 0).withPrecip(Precip.RAIN, 2.5);
        WetnessModel.Exposure e = WetnessModel.Exposure.of(rain, 1, Activity.REST, false, false);
        ClothingState bare = c, greased = c;
        for (int i = 0; i < 10; i++) {
            bare = WetnessModel.stepMinute(bare, e);
            greased = WetnessModel.stepMinute(greased, e, WellnessTuning.STANDARD.gloveWetMult());
        }
        assertTrue(bare.wet(HANDS_IN) > 0, "bare gloves wet in rain");
        assertEquals(bare.wet(HANDS_IN) * 0.7, greased.wet(HANDS_IN), 1e-9, "greased gloves gain x0.7");
        assertEquals(bare.wet(TORSO_IN), greased.wet(TORSO_IN), 1e-12, "only the hands change");
    }

    @Test
    void grease_lapses_with_the_clock() {
        BodyState s = new BodyState();
        s.gloveGreaseUntil = 100;
        assertEquals(0.7, Skin.gloveWetMult(s, 50, WellnessTuning.STANDARD), 1e-12);
        assertEquals(1.0, Skin.gloveWetMult(s, 100, WellnessTuning.STANDARD), 1e-12);
    }
}
