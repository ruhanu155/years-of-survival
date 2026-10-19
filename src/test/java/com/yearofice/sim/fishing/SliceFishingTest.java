package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.fishing.BiteModel.TimeBand;
import com.yearofice.sim.fishing.BiteModel.Weather;
import com.yearofice.sim.fishing.StrikeJudge.Cue;
import com.yearofice.sim.fishing.StrikeJudge.Hook;
import com.yearofice.sim.fishing.StrikeJudge.Result;
import org.junit.jupiter.api.Test;

/** The slice rod's pure parts: season, the spot rule, the bait, the strike, the close clock and the bite target. */
class SliceFishingTest {
    static final FishingTuning T = FishingTuning.standard();

    static SliceBite.Inputs slice(int day, TimeBand band, HookBait bait) {
        double k = T.graylingCapacity() * T.spotQ() * T.spawnCapacityX();
        return new SliceBite.Inputs(FishSpecies.arcticGrayling(), FishSeason.arcticGrayling(), "creek_mouth", true, T.spotQ(),
                1.89, T.spotRadiusM(), k, k, bait, day, band, Weather.CLEAR, T.waterTempC(), T.turbidity(), false);
    }

    @Test
    void calendarFromMay() {
        assertEquals(1, FishSeason.day(5, 1));
        assertEquals(153, FishSeason.day(9, 30));
        assertEquals(360, FishSeason.day(4, 25));
        assertEquals(365, FishSeason.day(4, 30));
        assertEquals(5, FishSeason.month(1));
        assertEquals(5, FishSeason.month(31));
        assertEquals(6, FishSeason.month(32));
        assertEquals(1, FishSeason.month(FishSeason.day(1, 15)));
        assertEquals(4, FishSeason.month(365));
        assertEquals(1, FishSeason.dayOfYear(366));
        for (int m = 1; m <= 12; m++) assertEquals(m, FishSeason.month(FishSeason.day(m, 10)));
    }

    @Test
    void graylingSeason() {
        FishSeason g = FishSeason.arcticGrayling();
        assertEquals(1.5, g.factor(FishSeason.day(5, 10), "creek_mouth"), 1e-12, "first month 0.5, ×3 at the spawn");
        assertEquals(0.5, g.factor(FishSeason.day(5, 10), "outside_bend"), 1e-12, "first month away from the spawn");
        assertEquals(1.0, g.factor(FishSeason.day(7, 1), "creek_mouth"), 1e-12);
        assertEquals(0.5, g.factor(FishSeason.day(9, 15), "creek_mouth"), 1e-12, "last month");
        assertEquals(0.3, g.factor(FishSeason.day(1, 15), "creek_mouth"), 1e-12, "resident outside the window");
        assertEquals(0.9, g.factor(FishSeason.day(4, 27), "creek_mouth"), 1e-12, "spawn before the window opens");
        FishSeason salmon = new FishSeason(FishSeason.day(7, 1), FishSeason.day(8, 31), false, false, null, 0, 0);
        assertEquals(0.0, salmon.factor(FishSeason.day(10, 1), "bend"), 0);
    }

    @Test
    void spotRule() {
        assertEquals(1 + 8 / 9.0, SpotRule.columnDepthM(2, 8 / 9.0), 1e-12);
        assertEquals(0.0, SpotRule.columnDepthM(0, 1), 0);
        SpotRule.Columns pool = (x, z) -> (x >= 0 && x <= 1 && z >= 0 && z <= 1) ? 1.89 : 0;
        assertTrue(SpotRule.isSpot(pool, 0, 0, 2, 1.5));
        assertTrue(SpotRule.isSpot(pool, 1, 1, 2, 1.5));
        assertFalse(SpotRule.isSpot(pool, 2, 2, 2, 1.5));
        assertFalse(SpotRule.isSpot(pool, 0, 0, 2, 2.0), "too shallow for a 2 m rule");
        SpotRule.Columns strip = (x, z) -> (z == 0 && x >= 0 && x <= 5) ? 3 : 0;
        assertFalse(SpotRule.isSpot(strip, 2, 0, 2, 1.5), "a one-column strip is not 2 × 2");
        SpotRule.Columns lake = (x, z) -> 4;
        assertTrue(SpotRule.isSpot(lake, 100, -40, 2, 1.5));
    }

    @Test
    void wormOnTheHook() {
        HookBait w = HookBait.fitted("worm", 2, 30);
        assertTrue(w.present());
        assertTrue(w.afterBite().present());
        assertFalse(w.afterBite().afterBite().present(), "2 bites");
        HookBait b = w;
        for (int i = 0; i < 29; i++) b = b.afterMinutes(1);
        assertTrue(b.present());
        assertFalse(b.afterMinutes(1).present(), "or 30 minutes");
        assertEquals(HookBait.NONE, b.afterMinutes(1));
        assertEquals(0.0, SliceBite.pBite(slice(1, TimeBand.DAY, HookBait.NONE)), 0, "a bare hook never bites");
    }

    @Test
    void strikeWindow() {
        Hook steel = Hook.steel();
        double g = T.strikeGraceS();
        assertEquals(Result.TWITCHED, StrikeJudge.judge(Cue.NONE, 0, steel, g, 60, 0, 0).result());
        assertFalse(StrikeJudge.judge(Cue.NONE, 0, steel, g, 60, 0, 0).baitGone(), "a twitch costs nothing");
        assertEquals(Result.HOOKED, StrikeJudge.judge(Cue.DIP, 0.3, steel, g, 60, 0.5, 0.5).result());
        assertEquals(Result.HOOK_LOST, StrikeJudge.judge(Cue.DIP, 0.3, steel, g, 60, 0.04, 0.9).result(), "steel loses 5%");
        assertEquals(Result.HOOKED, StrikeJudge.judge(Cue.DIP, 0.6 + g - 1e-9, steel, g, 60, 0.5, 0.5).result(), "grace");
        assertEquals(Result.LATE, StrikeJudge.judge(Cue.DIP, 0.6 + g + 0.01, steel, g, 60, 0.5, 0.5).result());
        assertEquals(Result.EARLY, StrikeJudge.judge(Cue.DIP, 0.1, Hook.woodGorge(), g, 60, 0.5, 0.5).result(), "a gorge sets late");
        StrikeJudge.Verdict nib = StrikeJudge.judge(Cue.NIBBLE, 0, steel, g, 60, 0.5, 0.59);
        assertTrue(nib.spookSpot() && nib.baitGone() && nib.result() == Result.SPOOKED);
        assertFalse(StrikeJudge.judge(Cue.NIBBLE, 0, steel, g, 60, 0.5, 0.61).baitGone(), "60% bait loss");
        assertEquals(Result.EARLY, StrikeJudge.judge(Cue.TWITCH, 0, steel, g, 60, 0.5, 0.5).result(), "struck at the twitch");
        int lost = 0;
        for (int i = 0; i < 10_000; i++) if (StrikeJudge.judge(Cue.NIBBLE, 0, steel, g, 60, 0, Draws.u(9, i, 0, 1)).baitGone()) lost++;
        assertEquals(6000, lost, 200);
    }

    @Test
    void closeClockRunsATenth() {
        CloseClock c = CloseClock.ZERO;
        long total = 0;
        for (int i = 0; i < 1000; i++) {
            CloseClock.Step s = c.tick(0.1);
            total += s.ticks();
            c = s.next();
        }
        assertEquals(100, total);
        assertEquals(1, CloseClock.ZERO.tick(1.0).ticks());
    }

    @Test
    void timeBands() {
        assertEquals(TimeBand.DAWN_DUSK, SliceBite.timeBand(6 * 60 + 30, 360, 1080, 24));
        assertEquals(TimeBand.DAWN_DUSK, SliceBite.timeBand(17 * 60 + 5, 360, 1080, 24));
        assertEquals(TimeBand.DAY, SliceBite.timeBand(8 * 60, 360, 1080, 24));
        assertEquals(TimeBand.MIDDAY, SliceBite.timeBand(12 * 60, 360, 1080, 24));
        assertEquals(TimeBand.NIGHT, SliceBite.timeBand(23 * 60, 360, 1080, 24));
        assertEquals(TimeBand.NIGHT, SliceBite.timeBand(4 * 60, 360, 1080, 24));
        assertEquals(TimeBand.DAWN_DUSK, SliceBite.timeBand(5 * 60, 360, 1080, 24), "dawn wins over night");
    }

    /**
     * The M2.5 target, in closed form and by draws: on a clear May morning at the slice spot a worm draws P_bite
     * 0.0907 a minute, so 94% of casts have a bite within 30 minutes. At dawn it is 99%; at midday 86%.
     */
    @Test
    void sliceBiteTarget() {
        SliceBite.Inputs day = slice(3, TimeBand.DAY, HookBait.fitted("worm", 2, 30));
        double p = SliceBite.pBite(day);
        assertEquals(0.12 * 0.84 * 0.6 * 1.5, p, 1e-12);
        double p30 = 1 - Math.pow(1 - p, 30);
        assertTrue(p30 >= 0.93 && p30 <= 0.95, "P30 " + p30);
        int hits = 0, n = 10_000;
        for (int i = 0; i < n; i++) if (SliceBite.firstBite(day, 1000 + i, 77, 480, 30) >= 0) hits++;
        assertEquals(p30, hits / (double) n, 0.01);
        assertTrue(1 - Math.pow(1 - SliceBite.pBite(slice(3, TimeBand.DAWN_DUSK, day.bait())), 30) > 0.98);
        assertTrue(1 - Math.pow(1 - SliceBite.pBite(slice(3, TimeBand.MIDDAY, day.bait())), 30) < 0.9);
        assertTrue(SliceBite.pBite(slice(40, TimeBand.DAY, day.bait())) < p, "June: no spawn bonus");
    }

    @Test
    void drawsAreStableAndUniform() {
        assertEquals(Draws.u(1, 2, 3, 4), Draws.u(1, 2, 3, 4), 0);
        double sum = 0;
        for (int i = 0; i < 100_000; i++) {
            double u = Draws.u(42, i, i * 31L, 1);
            assertTrue(u >= 0 && u < 1);
            sum += u;
        }
        assertEquals(0.5, sum / 100_000, 0.005);
        assertTrue(Draws.name("bite") != Draws.name("fight"));
    }

    @Test
    void presets() {
        assertEquals(1.5, FishingPreset.GENTLE.tuning.stockX(), 0);
        assertTrue(FishingPreset.GENTLE.tuning.assistOn());
        assertFalse(FishingPreset.STANDARD.tuning.assistOn());
        assertEquals(0.7, FishingPreset.BRUTAL.tuning.stockX(), 0);
        assertEquals(FishingPreset.STANDARD.tuning.spotQ(), FishingPreset.BRUTAL.tuning.spotQ(), 0);
    }
}
