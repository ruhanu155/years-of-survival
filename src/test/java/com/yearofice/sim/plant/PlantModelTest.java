package com.yearofice.sim.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlantModelTest {
    @Test
    void patchHealthRecovers() {
        double h = 1.0;
        h = PatchModel.nextHealth(h, 0.75, 0.7, false);
        assertEquals(0.5, h, 1e-9);
        h = PatchModel.nextHealth(h, 0.2, 0.7, false);
        assertEquals(0.75, h, 1e-9);
        h = PatchModel.nextHealth(h, 0.0, 0.7, false);
        assertEquals(1.0, h, 1e-9);
        assertEquals(1.0, PatchModel.nextHealth(h, 0.0, 0.7, false), 1e-9);
        assertEquals("worn", PatchModel.word(0.5));
        assertEquals(3, PatchModel.taken(1.3, 1.3));
        assertEquals(2, PatchModel.taken(1.0, 1.3));
        assertEquals(1, PatchModel.taken(0.5, 1.5));
    }

    @Test
    void notesIdentifyByThreeStagesOrNote() {
        PlantNotes n = PlantNotes.EMPTY;
        assertEquals(PlantNotes.Level.UNKNOWN, n.level("a"));
        assertEquals("a plant", n.shownName("a", "Cow parsnip"));
        n = n.seen("a", PlantNotes.SHOOT).seen("a", PlantNotes.FLOWER);
        assertEquals(PlantNotes.Level.PARTLY, n.level("a"));
        n = n.seen("a", PlantNotes.FRUIT);
        assertTrue(n.identified("a"));
        assertTrue(PlantNotes.EMPTY.identify("b").identified("b"));
        assertFalse(PlantNotes.EMPTY.identify("b").identified("a"));
    }

    @Test
    void phasesFollowDegreeDays() {
        Map<PlantPhase, Double> sums = Map.of(PlantPhase.BUD, 0.0, PlantPhase.LEAF, 40.0, PlantPhase.FLOWER, 90.0,
                PlantPhase.FRUIT, 420.0, PlantPhase.SEED, 640.0);
        assertEquals(PlantPhase.BUD, PhaseModel.phaseFor(sums, 0, 0, false));
        assertEquals(PlantPhase.LEAF, PhaseModel.phaseFor(sums, 60, 20, false));
        assertEquals(PlantPhase.FRUIT, PhaseModel.phaseFor(sums, PhaseModel.ddAt(70, 1.0), 70, false));
        assertEquals(PlantPhase.SENESCENT, PhaseModel.phaseFor(sums, 900, 130, false));
        assertEquals(PlantPhase.BURIED, PhaseModel.phaseFor(sums, 900, 200, true));
        assertEquals(120, PhaseModel.ddAt(31, 1.0), 1e-9);
        assertTrue(PhaseModel.ddAt(70, 0.9) < PhaseModel.ddAt(70, 1.0));
    }

    @Test
    void lickRegrowsDoubleAfterRain() {
        double g = 0;
        int since = 99;
        for (int d = 10; d < 17; d++) { g = LickModel.dayRegrow(g, 1000, d, false, since, 0.2); since = Math.min(99, since + 1); }
        assertEquals(200, g, 1.0);
        g = 0;
        g = LickModel.dayRegrow(g, 1000, 20, true, since, 0.2);
        since = 0;
        for (int d = 21; d < 28; d++) { g = LickModel.dayRegrow(g, 1000, d, false, since, 0.2); since++; }
        assertEquals(400, g, 1.0);
        assertEquals(0, LickModel.dayRegrow(0, 1000, 200, false, 99, 0.2), 1e-9);
        assertEquals(3, LickModel.crustState(600));
    }
}
