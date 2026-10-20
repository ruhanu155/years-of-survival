package com.yearofice.sim.hunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.hunt.Shot.Angle;
import com.yearofice.sim.hunt.Shot.Channel;
import com.yearofice.sim.hunt.Shot.Result;
import org.junit.jupiter.api.Test;

class HuntSimTest {
    static final HuntTuning T = HuntTuning.standard();

    @Test
    void weaponEnergiesMatchTheBlueprint() {
        assertEquals(37.5, HuntData.weapon("longbow").fullEnergyJ(), 1e-9);
        assertEquals(22.0, HuntData.weapon("self_bow").fullEnergyJ(), 0.6);
        assertEquals(50.0, HuntData.weapon("sinew_bow").fullEnergyJ(), 1.0);
    }

    /** 100 broadside heart-and-lung hits on a moose at 20 m, full-draw longbow, flint: none below the threshold; at 60% draw all wound. */
    @Test
    void broadsideLungShotsKillAndSixtyPercentDrawWounds() {
        HitZones z = HuntData.zones("moose");
        Weapon bow = HuntData.weapon("longbow");
        ArrowHead flint = HuntData.head("knapped_stone");
        int killed = 0, woundedAt60 = 0;
        for (int i = 0; i < 100; i++) {
            double y = 1.35 + (i % 10 - 5) * 0.01, zz = 0.55 - 0.1 + (i / 10) * 0.02; // behind the shoulder
            HitZones.Trace tr = z.trace("standing", new double[] {3, y, zz}, new double[] {-1, 0, 0}, 10);
            assertTrue(tr != null && tr.zone().name().equals("heart_lungs"), "lung zone: " + tr);
            Shot.Outcome full = Shot.resolve(tr.zone().name(), bow.fullEnergyJ(), flint, 20, 25, Angle.BROADSIDE, z.killThresholdJ(), HitZones.shoulder(tr), 0.99, T);
            if (full.result() == Result.KILL && full.channel() == Channel.VITALS) killed++;
            Shot.Outcome part = Shot.resolve(tr.zone().name(), bow.energyAt(0.6), flint, 20, 25, Angle.BROADSIDE, z.killThresholdJ(), false, 0.99, T);
            if (part.result() == Result.WOUND) woundedAt60++;
        }
        assertEquals(100, killed);
        assertEquals(100, woundedAt60);
    }

    @Test
    void rangeAngleAndShoulderRules() {
        ArrowHead flint = HuntData.head("knapped_stone"), steel = HuntData.head("steel"), bone = HuntData.head("bone");
        assertEquals(0.4, Shot.rangeFactor(40, 25, T), 1e-9);
        assertEquals(30.0, 37.5 * Shot.rangeFactor(30, 25, T), 1e-9); // a full-draw longbow still kills a moose at 30 m
        assertEquals(Angle.STRAIGHT_AWAY, Angle.of(0.9));
        Shot.Outcome behind = Shot.resolve("heart_lungs", 37.5, steel, 10, 25, Angle.STRAIGHT_AWAY, 30, false, 0.5, T);
        assertEquals(Channel.GUT, behind.channel());
        assertEquals(Channel.LEG, Shot.resolve("heart_lungs", 37.5, flint, 10, 25, Angle.BROADSIDE, 30, true, 0.10, T).channel());
        assertEquals(Channel.VITALS, Shot.resolve("heart_lungs", 37.5, flint, 10, 25, Angle.BROADSIDE, 30, true, 0.20, T).channel());
        assertEquals(Channel.VITALS, Shot.resolve("heart_lungs", 37.5, steel, 10, 25, Angle.BROADSIDE, 30, true, 0.01, T).channel());
        assertEquals(Channel.GUT, Shot.resolve("heart_lungs", 37.5, bone, 10, 25, Angle.QUARTERING_TOWARD, 30, false, 0.5, T).channel()); // 37.5*.85*.6 = 19
        assertEquals(Result.ANGER, Shot.resolve("gut", 37.5, HuntData.head("blunt"), 10, 25, Angle.BROADSIDE, 30, false, 0.5, T).result());
        assertEquals(Result.KILL, Shot.resolve("brain", 22, HuntData.head("blunt"), 5, 15, Angle.BROADSIDE, 2, false, 0.5, T).result());
    }

    @Test
    void woundTableAndPush() {
        WoundModel.Wound lung = WoundModel.open(Channel.VITALS, 0, 1, 0.5);
        assertTrue(lung.dieAfterMin() < 1 && lung.dropsPer10m() == 30 && lung.waitMin() == 15);
        WoundModel.Wound gut = WoundModel.open(Channel.GUT, 0, 1, 0.0);
        assertEquals(360, gut.dieAfterMin(), 1e-9);
        WoundModel.Wound pushed = gut.pushed(T);
        assertEquals(480, pushed.dieAfterMin(), 1e-9);
        assertEquals(0.5, pushed.trailMult(), 1e-9);
        WoundModel.Wound leg = WoundModel.open(Channel.LEG, 0, 1, 0.5);
        assertTrue(!leg.lethal() && leg.dropsPerM(99) > 0 && leg.dropsPerM(100) == 0);
        assertEquals(60, WoundModel.dropLifeMin(WoundModel.Weather.RAIN, T), 1e-9);
        assertEquals(240, WoundModel.dropLifeMin(WoundModel.Weather.SNOW, T), 1e-9);
    }

    @Test
    void ballisticsDropAndWind() {
        double[] none = Ballistics.dropAndDrift(50, 30, 0, T);
        double[] wind = Ballistics.dropAndDrift(50, 30, 6, T);
        assertTrue(none[0] > 0.5 && none[0] < 2.5, "drop " + none[0]);
        assertEquals(0, none[1], 1e-9);
        assertTrue(wind[1] > 0.05, "drift " + wind[1]);
    }

    @Test
    void callsFollowTheMooseFormula() {
        assertEquals(0.2, Calls.pRespond(0.4, Calls.rut(9, 20), 1, 1, false, 1, 400, 800, Calls.Falloff.LINEAR), 1e-9);
        assertEquals(0, Calls.pRespond(0.4, Calls.rut(7, 20), 1, 1, false, 1, 100, 800, Calls.Falloff.LINEAR), 1e-9);
        assertEquals(1200, Calls.effectiveRange(800, 1.3, 1.4, 1.2), 1e-9);
    }

    @Test
    void blindHalvesNoiseAndHidesStillPlayer() {
        Blinds.Effect e = Blinds.effect(Blinds.Kind.GROUND_BLIND, true, false, 30, true, 2, true, false, 0, T);
        assertEquals(0, e.sightX(), 1e-9);
        assertEquals(0.5, e.noiseX(), 1e-9);
        assertEquals(0.5, Blinds.effect(Blinds.Kind.GROUND_BLIND, true, true, 10, true, 2, true, false, 0, T).sightX(), 1e-9);
        assertEquals(1.0, Blinds.effect(Blinds.Kind.GROUND_BLIND, true, false, 30, true, 2, true, false, 50, T).sightX(), 1e-9);
    }
}
