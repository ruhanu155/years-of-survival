package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.env.ShelterState;
import org.junit.jupiter.api.Test;

/** Clothing › Wet and drying; Waters › Drying off; Warmth › Sweat and drying. */
class WetnessTest {
    private static final int TORSO_IN = Layer.slot(Zone.TORSO, Layer.INNER), TORSO_OUT = Layer.slot(Zone.TORSO, Layer.OUTER);
    private static final int FEET_IN = Layer.slot(Zone.FEET, Layer.INNER);

    @Test
    void swimmingSoaksEverySlotAndBelowZeroNothingDriesOnTheBody() {
        ClothingState c = WetnessModel.stepMinute(Wardrobe.start(), WetnessModel.Exposure.dry(-20).in(ColdWater.Depth.SWIM));
        for (int i = 0; i < Layer.SLOTS; i++) assertEquals(1.0, c.wet(i), 1e-12);
        assertTrue(Clothing.soaked(c));
        ClothingState later = WetnessModel.step(c, WetnessModel.Exposure.dry(-20).doing(Activity.WALK, true), 60);
        assertEquals(1.0, Clothing.wetness(later), 1e-12);
    }

    @Test
    void byTheFireClothesDryInAboutAnHour() {
        ClothingState c = ClothingState.of(Wardrobe.FLEECE, Wardrobe.WORK_PANTS).withAllWet(1);
        WetnessModel.Exposure fire = WetnessModel.Exposure.dry(-20).drying(WetnessModel.Drying.FIRE);
        assertEquals(0.5, WetnessModel.step(c, fire, 30).wet(TORSO_IN), 1e-9);
        assertEquals(0.0, WetnessModel.step(c, fire, 60).wet(TORSO_IN), 1e-9);
    }

    @Test
    void onTheBodyDryingRunsTenFiveAndZeroPercentAnHour() {
        ClothingState c = ClothingState.of(Wardrobe.FLEECE).withAllWet(1);
        assertEquals(0.9, WetnessModel.step(c, WetnessModel.Exposure.dry(15).doing(Activity.WALK, true), 60).wet(TORSO_IN), 1e-9);
        assertEquals(0.95, WetnessModel.step(c, WetnessModel.Exposure.dry(5).doing(Activity.WALK, true), 60).wet(TORSO_IN), 1e-9);
        assertEquals(1.0, WetnessModel.step(c, WetnessModel.Exposure.dry(-1).doing(Activity.WALK, true), 60).wet(TORSO_IN), 1e-9);
    }

    @Test
    void theLineDriesInAboutFourHours() {
        assertEquals(0.5, WetnessModel.dryLoose(1, WetnessModel.Drying.BODY, true, 120), 1e-12);
        assertEquals(0.0, WetnessModel.dryLoose(1, WetnessModel.Drying.BODY, true, 240), 1e-12);
    }

    @Test
    void anInnerLayerUnderAWetOuterDoesNotDry() {
        ClothingState c = ClothingState.of(Wardrobe.FLEECE, Wardrobe.HIDE_COAT).withAllWet(1);
        ClothingState after = WetnessModel.step(c, WetnessModel.Exposure.dry(-5).drying(WetnessModel.Drying.FIRE), 30);
        assertEquals(0.5, after.wet(TORSO_OUT), 1e-9);
        assertEquals(1.0, after.wet(TORSO_IN), 1e-9);
        ClothingState coatOff = after.remove(Zone.TORSO, Layer.OUTER);
        assertEquals(0.5, WetnessModel.step(coatOff, WetnessModel.Exposure.dry(-5).drying(WetnessModel.Drying.FIRE), 30)
                .wet(TORSO_IN), 1e-9);
    }

    @Test
    void theRainShellShedsRainUntilBelowHalfCondition() {
        EnvSample rain = EnvSample.outdoors(8, 0).withPrecip(Precip.RAIN, 2.5);
        WetnessModel.Exposure e = WetnessModel.Exposure.of(rain, 1, Activity.WALK, true, false);
        ClothingState shell = WetnessModel.step(ClothingState.of(Wardrobe.FLEECE, Wardrobe.RAIN_SHELL), e, 60);
        assertEquals(0, shell.wet(TORSO_IN), 1e-12);
        assertEquals(0, shell.wet(TORSO_OUT), 1e-12);

        ClothingState worn = ClothingState.of(Wardrobe.FLEECE).wear(new WornGarment(Wardrobe.RAIN_SHELL, 0.4));
        ClothingState leaky = WetnessModel.step(worn, e, 60);
        assertEquals(1.0, leaky.wet(TORSO_OUT), 1e-9);
        assertTrue(leaky.wet(TORSO_IN) > 0.3);
    }

    @Test
    void moderateRainSoaksAnUnprotectedLayerInHalfAnHourAndALeanToHalvesIt() {
        EnvSample rain = EnvSample.outdoors(8, 0).withPrecip(Precip.RAIN, 2.5);
        ClothingState c = ClothingState.of(Wardrobe.FLEECE);
        assertEquals(1.0, WetnessModel.step(c, WetnessModel.Exposure.of(rain, 1, Activity.REST, false, false), 30).wet(TORSO_IN), 1e-9);
        assertEquals(0.5, WetnessModel.step(c,
                WetnessModel.Exposure.of(rain, ShelterState.LEAN_TO.rainFactor(), Activity.REST, false, false), 30).wet(TORSO_IN), 1e-9);
        assertEquals(0, WetnessModel.step(c,
                WetnessModel.Exposure.of(rain.inRoom(3, 15), 1, Activity.REST, false, false), 30).wet(TORSO_IN), 1e-9);
    }

    @Test
    void snowWetsOnlyAsItMelts() {
        ClothingState c = ClothingState.of(Wardrobe.FLEECE);
        EnvSample cold = EnvSample.outdoors(-5, 0).withPrecip(Precip.SNOW, 3);
        assertEquals(0, WetnessModel.step(c, WetnessModel.Exposure.of(cold, 1, Activity.WALK, true, false), 60).wet(TORSO_IN), 1e-12);
        EnvSample wet = EnvSample.outdoors(1, 0).withPrecip(Precip.SNOW, 3);
        assertTrue(WetnessModel.step(c, WetnessModel.Exposure.of(wet, 1, Activity.WALK, true, false), 10).wet(TORSO_IN) > 0);
    }

    @Test
    void waterproofBootsKeepAnkleDeepFeetDryButNotKneeDeep() {
        ClothingState c = ClothingState.of(Wardrobe.WOOL_SOCKS, Wardrobe.HIKING_BOOTS);
        assertEquals(0, WetnessModel.stepMinute(c, WetnessModel.Exposure.dry(5).in(ColdWater.Depth.ANKLE)).wet(FEET_IN), 1e-12);
        assertEquals(1, WetnessModel.stepMinute(c, WetnessModel.Exposure.dry(5).in(ColdWater.Depth.KNEE)).wet(FEET_IN), 1e-12);
        ClothingState moccasins = ClothingState.of(Wardrobe.WOOL_SOCKS, Wardrobe.MOCCASINS);
        assertEquals(1, WetnessModel.stepMinute(moccasins, WetnessModel.Exposure.dry(5).in(ColdWater.Depth.ANKLE)).wet(FEET_IN), 1e-12);
    }

    @Test
    void sweatWetsInnerLayersUnderHeavyClothingUnlessTheCoatIsOpen() {
        ClothingState c = Wardrobe.october();
        assertTrue(Clothing.warmthC(c) > 20);
        WetnessModel.Exposure chopping = WetnessModel.Exposure.dry(-5).doing(Activity.HEAVY, true);
        assertEquals(0.10, WetnessModel.stepMinute(c, chopping).wet(TORSO_IN), 1e-12);
        assertEquals(0, WetnessModel.stepMinute(c.withCoatOpen(true), chopping).wet(TORSO_IN), 1e-12);
        assertEquals(0, WetnessModel.stepMinute(c, WetnessModel.Exposure.dry(-15).doing(Activity.HEAVY, true)).wet(TORSO_IN), 1e-12);
        assertEquals(0, WetnessModel.stepMinute(c, WetnessModel.Exposure.dry(-5).doing(Activity.WALK, true)).wet(TORSO_IN), 1e-12);
    }

    @Test
    void wetClothesWeigh() {
        ClothingState c = ClothingState.of(Wardrobe.PARKA, Wardrobe.HIDE_PANTS, Wardrobe.MUKLUKS).withAllWet(1);
        assertEquals(7.7, Clothing.waterWeightKg(c), 1e-9); // the Blueprint's "about 8 kg"
    }

    @Test
    void rollingInPowderSnowWicksTwentyPercent() {
        ClothingState c = WetnessModel.wick(ClothingState.of(Wardrobe.FLEECE).withAllWet(1), 0.2);
        assertEquals(0.8, c.wet(TORSO_IN), 1e-12);
    }
}
