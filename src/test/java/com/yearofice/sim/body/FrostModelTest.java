package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.body.part.Part;
import org.junit.jupiter.api.Test;

/** B13/B14 on the frost limits, and the fever and Rest-day rates. */
class FrostModelTest {

    @Test
    void face_bonus_from_grease_and_beard() {
        ClothingState none = ClothingState.of(Wardrobe.WORK_PANTS);
        WellnessTuning w = WellnessTuning.STANDARD;
        BodyState s = new BodyState();
        double base = FrostModel.partLimitC(none, Part.HEAD, false, Skin.faceBonusC(s, 0, w));
        assertEquals(-20, base, 1e-12, "bare face: -20");
        s.faceGreaseUntil = 480;
        double greased = FrostModel.partLimitC(none, Part.HEAD, false, Skin.faceBonusC(s, 0, w));
        assertEquals(-22, greased, 1e-12, "grease: 2 C colder");
        s.facialHair = true;
        s.hairDays = 2 * w.beardStageDays();
        assertEquals(-24, FrostModel.partLimitC(none, Part.HEAD, false, Skin.faceBonusC(s, 0, w)), 1e-12, "grease and a Full beard stack");
        s.beardIced = true;
        assertEquals(-22, FrostModel.partLimitC(none, Part.HEAD, false, Skin.faceBonusC(s, 0, w)), 1e-12, "an iced beard gives nothing");
        assertEquals(-20, FrostModel.partLimitC(none, Part.HEAD, false, Skin.faceBonusC(s, 600, w)), 1e-12, "grease worn off");
        assertEquals(-20, FrostModel.partLimitC(none, Part.TORSO, false, 4), 1e-12, "only the face gets it");
        assertTrue(FrostModel.tNipMinutes(greased, -26) > FrostModel.tNipMinutes(base, -26), "frostnip comes later");
    }

    @Test
    void sleep_and_rest_day_rates() {
        WellnessTuning w = WellnessTuning.STANDARD;
        BodyState s = new BodyState();
        assertEquals(1.0, SleepRules.feverFactor(s, 10, w), 1e-12);
        IllnessModel.start(s, "fever", 0, 1440);
        assertEquals(0.7, SleepRules.feverFactor(s, 10, w), 1e-12);
        assertEquals(1.0, SleepRules.feverFactor(s, 1500, w), 1e-12);
        assertEquals(2.0, RestDay.problemRate("sprain", w), 1e-12);
        assertEquals(1.5, RestDay.problemRate("broken_bone", w), 1e-12);
        assertEquals(1.0, RestDay.problemRate("cut", w), 1e-12);
    }
}
