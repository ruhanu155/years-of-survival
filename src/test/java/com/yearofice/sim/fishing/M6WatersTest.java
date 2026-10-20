package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M6 lane C: salmon runs by date, the echo, spot migration, the weir fence and the knife's chunks. */
class M6WatersTest {
    @Test
    void chumRunIsATriangleOnItsWindow() {
        int jul1 = FishSeason.day(7, 1), jul15 = FishSeason.day(7, 15), aug5 = FishSeason.day(8, 5), aug31 = FishSeason.day(8, 31), sep5 = FishSeason.day(9, 5);
        assertEquals(0.0, SalmonRun.strength("chum_salmon", jul1));
        assertEquals(1.0, SalmonRun.strength("chum_salmon", aug5), 1e-9);
        assertTrue(SalmonRun.strength("chum_salmon", jul15) > 0 && SalmonRun.strength("chum_salmon", jul15) < 0.1);
        assertTrue(SalmonRun.strength("chum_salmon", FishSeason.day(7, 25)) < SalmonRun.strength("chum_salmon", FishSeason.day(8, 1)));
        assertTrue(SalmonRun.strength("chum_salmon", aug31) > 0);
        assertEquals(0.0, SalmonRun.strength("chum_salmon", sep5));
        assertEquals(1.0, SalmonRun.strength("coho_salmon", sep5), 1e-9);
        assertEquals(1.0, SalmonRun.strength("chinook_salmon", FishSeason.day(7, 10)), 1e-9);
        assertEquals(1.0, SalmonRun.strength("sockeye_salmon", FishSeason.day(8, 1)), 1e-9);
    }

    @Test
    void yearFactorIsTriangularWithMeanOne() {
        double sum = 0, lo = 9, hi = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            double y = SalmonRun.yearFactor((i + 0.5) / n);
            sum += y;
            lo = Math.min(lo, y);
            hi = Math.max(hi, y);
        }
        assertEquals(1.0, sum / n, 0.01);
        assertTrue(lo >= 0.5 && hi <= 1.6);
    }

    @Test
    void echoFollowsTheHarvestShare() {
        assertEquals(1.0, FishStock.echo(0.2), 1e-9);
        assertEquals(0.85, FishStock.echo(0.3), 1e-9);
        assertEquals(0.4, FishStock.echo(0.9), 1e-9);
        assertEquals(0.4, SalmonRun.runSize(7L, "chum_salmon", 9, 5.0) / (SalmonRun.RUNS.get("chum_salmon").rMean() * SalmonRun.yearFactor(7L, "chum_salmon", 9)), 1e-9);
    }

    @Test
    void migrationRefillsAFishedDownSpot() {
        double k = 20;
        assertEquals(0.0, SpotMigration.after(0, k, 0.08, 0));
        double d7 = SpotMigration.after(0, k, 0.08, 7), d14 = SpotMigration.after(0, k, 0.08, 14);
        assertTrue(d7 > 0.35 * k && d7 < 0.5 * k, "d7 " + d7);
        assertTrue(d14 > 0.6 * k && d14 <= k, "d14 " + d14);
        assertEquals(k, SpotMigration.after(k, k, 0.08, 30));
        assertEquals(0.0, SpotMigration.after(0, k, 0.0, 30));
        assertTrue(SpotMigration.migrates("creek_mouth") && !SpotMigration.migrates("tarn_shoal") && !SpotMigration.migrates("beaver_pond"));
    }

    @Test
    void weirFenceRaisesTheBasket() {
        assertEquals(1.0, WeirFence.multiplier(0, 12), 1e-9);
        assertEquals(1.0, WeirFence.multiplier(2, 12), 1e-9, "two stakes are not a fence");
        assertTrue(WeirFence.multiplier(8, 12) > WeirFence.multiplier(5, 12));
        assertEquals(2.5, WeirFence.multiplier(30, 12), 1e-9);
        assertFalse(WeirFence.coverage(8, 12) > 1);
        assertEquals(2, FishCut.chunks(100));
        assertEquals(4, FishCut.chunks(600));
    }
}
