package com.yearofice.sim.bird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** M6 lane C: the birds' pure rules. */
class BirdRulesTest {
    static final BirdTuning T = BirdTuning.standard();

    @Test
    void rolesAndFlocks() {
        assertEquals(BirdRules.Role.CORVID, BirdRules.role("raven"));
        assertEquals(BirdRules.Role.JAY, BirdRules.role("stellers_jay"));
        assertNull(BirdRules.role("moose"));
        assertTrue(BirdRules.flocks(BirdRules.Role.DABBLER) && BirdRules.flocks(BirdRules.Role.GULL) && !BirdRules.flocks(BirdRules.Role.RAPTOR));
        assertEquals(T.dabblerMin(), BirdRules.flockSize(BirdRules.Role.DABBLER, T, 0.0));
        assertEquals(T.dabblerMax(), BirdRules.flockSize(BirdRules.Role.DABBLER, T, 0.9999));
        assertEquals(1, BirdRules.flockSize(BirdRules.Role.OWL, T, 0.5));
    }

    @Test
    void aMemberSteersBackToItsFlock() {
        double[] d = BirdRules.steer(30, 0, List.of(new double[] {0, 0}, new double[] {1, 1}), T, 3.0);
        assertTrue(d[0] < -2.5, "toward the centre: " + d[0]);
        double[] stay = BirdRules.steer(4, 0, List.of(new double[] {0, 0}, new double[] {8, 0}), T, 3.0);
        assertEquals(0.0, Math.hypot(stay[0], stay[1]), 1e-9);
        double[] apart = BirdRules.steer(0.5, 0, List.of(new double[] {0, 0}, new double[] {1, 0}), T, 3.0);
        assertTrue(Math.hypot(apart[0], apart[1]) < 3.0 + 1e-9);
    }

    @Test
    void scavengersArriveAndEat() {
        assertTrue(BirdRules.arrivalDelayMin(BirdRules.Role.CORVID, T, 0.99) <= T.corvidArriveMin());
        assertEquals(1000.0 / 1440.0 * 60, BirdRules.eatGrams(BirdRules.Role.CORVID, T, 60), 1e-9);
        assertTrue(BirdRules.eatGrams(BirdRules.Role.DABBLER, T, 60) == 0);
    }

    @Test
    void eagleTakesMoreFromAFullerSpot() {
        double full = BirdRules.eagleTakeChance(T, 1.0, 1440), half = BirdRules.eagleTakeChance(T, 0.5, 1440), none = BirdRules.eagleTakeChance(T, 0.0, 1440);
        assertTrue(full > half && half > none && none == 0.0);
        assertEquals(1.0 - Math.pow(1.0 - T.eagleFishPerDay() / 1440.0, 1440), full, 1e-9);
    }

    @Test
    void seasonsAndHours() {
        assertTrue(!BirdRules.inSeason("northern_pintail", 1) && BirdRules.inSeason("northern_pintail", 5) && BirdRules.inSeason("raven", 1));
        assertTrue(BirdRules.active(BirdRules.Role.OWL, 2) && !BirdRules.active(BirdRules.Role.OWL, 12) && BirdRules.active(BirdRules.Role.CORVID, 12));
    }

    @Test
    void drinkingPeaksAtDawnAndDusk() {
        assertTrue(DrinkSchedule.strength(6 * 60) > DrinkSchedule.strength(12 * 60));
        assertTrue(DrinkSchedule.strength(19 * 60) > DrinkSchedule.strength(12 * 60));
        assertEquals(1.0, DrinkSchedule.strength(6 * 60), 1e-9);
        assertEquals(0.25, DrinkSchedule.strength(0), 1e-9);
    }
}
