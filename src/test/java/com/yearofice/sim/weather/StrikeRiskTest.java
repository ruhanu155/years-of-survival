package com.yearofice.sim.weather;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StrikeRiskTest {
    @Test
    void dialIsTheChanceAtThePointAndFadesWithDistanceAndRoofs() {
        assertEquals(0.3, StrikeRisk.chance(0, 0.3, true), 1e-12);
        assertEquals(0.15, StrikeRisk.chance(7.5, 0.3, true), 1e-12);
        assertEquals(0, StrikeRisk.chance(15, 0.3, true));
        assertEquals(0, StrikeRisk.chance(1, 0.3, false));
    }
}
