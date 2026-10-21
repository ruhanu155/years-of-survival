package com.yearofice.sim.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.fire.ControlledBurn;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** The world's slow answers: wildfire escalation, regrowth, coppice, the exclosure, beaver dams, the controlled burn, the old camp. */
class LongWorldTest {
    static final LongWorldTuning STD = LongWorldTuning.Preset.STANDARD.tuning;

    /** {@code yearofice:world/wildfire_escalation}: 1,000 strikes, 0 escalate in ordinary years below Extreme, 20 to 40% in wildfire years. */
    @Test
    void wildfire_escalation() {
        SplittableRandom r = new SplittableRandom(2026);
        int ordinary = 0, wild = 0, extreme = 0;
        for (int i = 0; i < 1000; i++) {
            if (Wildfire.escalates(false, Wildfire.Danger.HIGH, r, STD)) ordinary++;
            if (Wildfire.escalates(true, Wildfire.Danger.MODERATE, r, STD)) wild++;
            if (Wildfire.escalates(false, Wildfire.Danger.EXTREME, r, STD)) extreme++;
        }
        assertEquals(0, ordinary);
        assertTrue(wild >= 200 && wild <= 400, "wildfire year: " + wild);
        assertTrue(extreme > 400, "Extreme danger escalates in an ordinary year too: " + extreme);
        int years = 0;
        for (int y = 1; y <= 2000; y++) if (Wildfire.wildfireYear(99, y, STD)) years++;
        assertEquals(0.20, years / 2000.0, 0.03);
        assertTrue(Wildfire.classFor(Wildfire.Danger.EXTREME, 3).cells > Wildfire.classFor(Wildfire.Danger.MODERATE, 3).cells);
        assertEquals(Wildfire.FireClass.SMOULDER, Wildfire.classFor(Wildfire.Danger.LOW, 14));
        assertEquals(Wildfire.FireClass.CROWN, Wildfire.classFor(Wildfire.Danger.HIGH, 12).ordinal() >= 2 ? Wildfire.FireClass.CROWN : null);
    }

    /** {@code yearofice:fire/controlled_burn}: inside the window with a 10 m break under 5% reach spruce; a July dry spell with none, 40% or more. */
    @Test
    void controlled_burn() {
        SplittableRandom r = new SplittableRandom(5);
        int windowBreak = 0, julyNoBreak = 0;
        for (int i = 0; i < 100; i++) {
            if (ControlledBurn.reachesSpruce(ControlledBurn.Day.window(), 10, r)) windowBreak++;
            if (ControlledBurn.reachesSpruce(ControlledBurn.Day.julyDry(), 0, r)) julyNoBreak++;
        }
        assertTrue(windowBreak < 5, "window with a break: " + windowBreak);
        assertTrue(julyNoBreak >= 40, "July dry spell, no break: " + julyNoBreak);
        // the break matters even in a dry spell
        int julyBreak = 0;
        for (int i = 0; i < 1000; i++) if (ControlledBurn.reachesSpruce(ControlledBurn.Day.julyDry(), 10, r)) julyBreak++;
        assertTrue(julyBreak / 1000.0 < julyNoBreak / 100.0 + 0.0001 || julyNoBreak == 100);
    }

    @Test
    void regrowth_follows_the_blueprint_dates() {
        long cleared = 40; // June 10 of year 1
        long june1 = Regrowth.firstJune1(cleared);
        assertEquals(365 + 31, june1, "the first June 1 after a June 10 clearing is next year's");
        assertEquals(31, Regrowth.firstJune1(0));
        assertEquals(31 + 365, Regrowth.firstJune1(31));
        assertEquals(Regrowth.Stage.CLEARED, Regrowth.stageAt(cleared, june1 - 1, 1));
        assertEquals(Regrowth.Stage.FIREWEED, Regrowth.stageAt(cleared, june1, 1));
        assertEquals(Regrowth.Stage.SAPLINGS, Regrowth.stageAt(cleared, cleared + 2 * 365, 1));
        assertEquals(Regrowth.Stage.SPRUCE_SAPLINGS, Regrowth.stageAt(cleared, cleared + 6 * 365, 1));
        assertEquals(Regrowth.Stage.BIRCH_LOGS, Regrowth.stageAt(cleared, cleared + 20 * 365, 1));
        assertEquals(Regrowth.Stage.FIREWEED, Regrowth.stageAt(cleared, cleared + 2 * 365 - 1, 1));
        // a Brutal clock is slower
        assertEquals(Regrowth.Stage.FIREWEED, Regrowth.stageAt(cleared, cleared + 2 * 365, 1.2));
        int fire = 0;
        for (int x = 0; x < 100; x++) for (int z = 0; z < 100; z++) if (Regrowth.cellGrows(1, x, z, Regrowth.Stage.FIREWEED)) fire++;
        assertEquals(0.5, fire / 10000.0, 0.03);
    }

    /** {@code yearofice:forage/coppice_shoots}: the willow stool and the birch that dies on a second cut. */
    @Test
    void coppice_shoots() {
        int oct15 = 153 + 14;
        long cut = oct15;
        long june15 = Regrowth.shootsShowDay(cut);
        assertEquals(365 + 45, june15);
        for (int i = 0; i < 200; i++) {
            int shoots = Regrowth.shoots(7, i, i * 3, Regrowth.Coppice.WILLOW);
            assertTrue(shoots >= 6 && shoots <= 12, "shoots " + shoots);
            assertEquals(0, Regrowth.poleHeightM(7, i, i * 3, cut, june15 - 1), 1e-9);
            long thirdMay1 = 3 * 365; // the third May 1 after an October cut in year 1
            double h = Regrowth.poleHeightM(7, i, i * 3, cut, thirdMay1);
            assertTrue(h >= 3.0 && h <= 4.0, "pole height at the third May 1: " + h);
        }
        int died = 0;
        for (int i = 0; i < 2000; i++) {
            assertTrue(!Regrowth.diesOnCut(3, i, 5, 1, Regrowth.Coppice.BIRCH));
            if (Regrowth.diesOnCut(3, i, 5, 2, Regrowth.Coppice.BIRCH)) died++;
        }
        // 2,000 stools at 50%: three standard deviations is 47 to 53 percent.
        assertTrue(died >= 940 && died <= 1060, "birch stools dead after the second cut: " + died);
        for (int i = 0; i < 100; i++) assertTrue(!Regrowth.diesOnCut(3, i, 5, 5, Regrowth.Coppice.WILLOW));
    }

    /** {@code yearofice:forage/browse_exclosure}: 8 stems, 20 moose-days, share 0.63; unfenced under 3 m after 5 years, fenced 4.5 to 5.5. */
    @Test
    void browse_exclosure() {
        double share = Regrowth.browseShare(20, 8);
        assertEquals(0.625, share, 0.01);
        double unfenced = Regrowth.heightAfter(1.5, 5, share);
        double fenced = Regrowth.heightAfter(1.5, 5, 0);
        assertTrue(unfenced < 3.0, "unfenced " + unfenced);
        assertTrue(fenced >= 4.5 && fenced <= 5.5, "fenced " + fenced);
        assertTrue(Regrowth.heightAfter(1.5, 3, 0) - Regrowth.heightAfter(1.5, 3, share) > 1.7, "about 2 m taller in 3 summers");
        assertEquals(0.9, Regrowth.browseShare(500, 8), 1e-9, "capped at 0.9");
    }

    /** {@code yearofice:world/beaver_pond_rise} (sim half): 40 willows raise the dam two stages in two years, the pond one block. */
    @Test
    void beaver_dam_rises() {
        int stage = 0;
        for (int y = 0; y < 2; y++) stage = BeaverDam.nextStage(stage, 40, false);
        assertEquals(2, stage);
        assertEquals(1.0, BeaverDam.riseM(stage), 1e-9);
        assertEquals(1, BeaverDam.riseBlocks(stage));
        assertEquals(0, BeaverDam.riseBlocks(1));
        assertEquals(1, BeaverDam.nextStage(1, 29, false), "29 trees hold the dam where it is");
        assertEquals(4, BeaverDam.nextStage(4, 99, false));
        assertEquals(3, BeaverDam.nextStage(4, 99, true), "an abandoned pond drains a stage a year");
        assertEquals(2, BeaverDam.growDays(0, 2 * 365).size());
        assertEquals(BeaverDam.GROW_DOY, BeaverDam.growDays(0, 365).get(0).intValue());
    }

    /** {@code old_camp_10y}: 50 seeds, half the sod roof down; peeled on stone under 5% rotten, unpeeled on soil 25 to 35%. */
    @Test
    void old_camp_ten_years() {
        double roofRate = 6.7, peeledStone = 0.4, unpeeledSoil = 3.5;
        int roofs = 0, roofDown = 0, a = 0, aRot = 0, b = 0, bRot = 0;
        for (long seed = 1; seed <= 50; seed++) {
            for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) {
                roofs++;
                if (OldCamp.lost(seed, x, 4, z, roofRate, 10)) roofDown++;
            }
            for (int y = 1; y <= 3; y++) for (int i = 0; i < 20; i++) {
                a++;
                if (OldCamp.lost(seed, i, y, 0, peeledStone, 10)) aRot++;
                b++;
                if (OldCamp.lost(seed, i, y, 7, unpeeledSoil, 10)) bRot++;
            }
        }
        double roof = roofDown / (double) roofs;
        assertTrue(roof >= 0.40 && roof <= 0.60, "roof down " + roof);
        assertTrue(aRot / (double) a < 0.05, "peeled on stone " + aRot / (double) a);
        double soil = bRot / (double) b;
        assertTrue(soil >= 0.25 && soil <= 0.35, "unpeeled on soil " + soil);
        // the same seed, the same camp
        for (int i = 0; i < 100; i++) assertEquals(OldCamp.lost(9, i, 2, i, 5, 10), OldCamp.lost(9, i, 2, i, 5, 10));
        assertEquals(0.0, OldCamp.chance(0, 30), 1e-12);
        assertTrue(OldCamp.chance(6.7, 30) > OldCamp.chance(6.7, 10));
    }
}
