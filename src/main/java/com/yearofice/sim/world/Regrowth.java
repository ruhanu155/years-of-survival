package com.yearofice.sim.world;

import java.util.SplittableRandom;

/**
 * Cleared ground answers (The open world › Regrowth), the coppice stool and the browse line (Plants and fungi › Browse,
 * the exclosure). Pure. Days are absolute game days; day-of-year 0 is May 1, so June 1 is day 31.
 */
public final class Regrowth {
    private Regrowth() {}

    public static final int JUNE_1 = 31, YEAR = 365;

    public enum Stage { CLEARED, FIREWEED, SAPLINGS, SPRUCE_SAPLINGS, BIRCH_POLES, BIRCH_LOGS }

    /** The first June 1 after {@code clearedDay} (a clearing made on June 1 itself waits for the next). */
    public static long firstJune1(long clearedDay) {
        long d = clearedDay + 1;
        int doy = (int) Math.floorMod(d, (long) YEAR);
        return d + Math.floorMod(JUNE_1 - doy, YEAR);
    }

    /** The day a stage begins: the first June 1; 2 years (birch and willow saplings); 6 years (spruce); 8 and 20 (birch poles, logs). */
    public static long stageDay(long clearedDay, Stage s, double timeScale) {
        return switch (s) {
            case CLEARED -> clearedDay;
            case FIREWEED -> firstJune1(clearedDay);
            case SAPLINGS -> clearedDay + Math.round(2 * YEAR * timeScale);
            case SPRUCE_SAPLINGS -> clearedDay + Math.round(6 * YEAR * timeScale);
            case BIRCH_POLES -> clearedDay + Math.round(8 * YEAR * timeScale);
            case BIRCH_LOGS -> clearedDay + Math.round(20 * YEAR * timeScale);
        };
    }

    public static Stage stageAt(long clearedDay, long today, double timeScale) {
        Stage best = Stage.CLEARED;
        for (Stage s : Stage.values()) if (today >= stageDay(clearedDay, s, timeScale)) best = s;
        return best;
    }

    /** Share of cells that carry a plant at a stage (a thicket is not a lawn). */
    public static double density(Stage s) {
        return switch (s) { case CLEARED -> 0; case FIREWEED -> 0.5; case SAPLINGS -> 0.2; case SPRUCE_SAPLINGS -> 0.1; case BIRCH_POLES, BIRCH_LOGS -> 0.1; };
    }

    /** A stable roll for one cell and stage, the same live and in catch-up. */
    public static boolean cellGrows(long seed, int x, int z, Stage s) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L + s.ordinal() * 0x27D4EB2F165667C5L;
        return new SplittableRandom(h).nextDouble() < density(s);
    }

    // ---- coppice ------------------------------------------------------------------------------------------------

    public enum Coppice {
        /** Feltleaf and other willows: shoot every cut, for ever. */ WILLOW(6, 12, 0.0),
        /** Birch coppices weakly: 50% of stools die after a second cut. */ BIRCH(3, 6, 0.5);

        public final int shootsLo, shootsHi;
        public final double deathAtSecondCut;

        Coppice(int lo, int hi, double d) { shootsLo = lo; shootsHi = hi; deathAtSecondCut = d; }
    }

    /** Shoots a stool sends up the next June (6–12 for a willow), fixed by the stool's place. */
    public static int shoots(long seed, int x, int z, Coppice c) {
        SplittableRandom r = new SplittableRandom(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL));
        return c.shootsLo + r.nextInt(c.shootsHi - c.shootsLo + 1);
    }

    /** Does the stool die on its {@code cuts}-th cut? One draw per stool and cut. */
    public static boolean diesOnCut(long seed, int x, int z, int cuts, Coppice c) {
        if (cuts < 2) return false;
        return new SplittableRandom(seed * 31 + cuts ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0x165667B19E3779F9L)).nextDouble() < c.deathAtSecondCut;
    }

    /** The first June 15 on or after a cut day: shoots show then. */
    public static long shootsShowDay(long cutDay) {
        int june15 = JUNE_1 + 14;
        long d = cutDay + 1;
        return d + Math.floorMod(june15 - (int) Math.floorMod(d, (long) YEAR), YEAR);
    }

    /** Pole height of a willow stool: 4.6 m ceiling, the stool's own vigour 0.9–1.1; zero before the shoots show. */
    public static double poleHeightM(long seed, int x, int z, long cutDay, long today) {
        long show = shootsShowDay(cutDay);
        if (today < show) return 0;
        double years = (today - show) / (double) YEAR;
        double vigour = 0.9 + 0.2 * new SplittableRandom(seed ^ (x * 31L) ^ (z * 17L)).nextDouble();
        return 4.6 * vigour * (1 - Math.exp(-(years + 0.25) / 1.45));
    }

    // ---- browse and the exclosure -------------------------------------------------------------------------------------

    public static final double BROWSE_CAP = 0.9, REACH_M = 3.0, HMAX_M = 6.0, R_M_PER_YEAR = 1.4, DIEBACK_M = 0.4, MIN_M = 0.5;

    /** Browse share of a cell: moose-days × 0.25 ÷ stems, capped at 0.9. */
    public static double browseShare(double mooseDaysPerWinter, int stems) { return Math.min(BROWSE_CAP, mooseDaysPerWinter * 0.25 / Math.max(1, stems)); }

    /**
     * One year of a young willow stem's height. A stem under 3 m loses {@code share} of its growth to browse; over 50%
     * browse three winters running brooms it, and growth halves from then on. A fenced stem has share 0.
     */
    public static double growYear(double heightM, double share, int broomedWinters) {
        double g = R_M_PER_YEAR * (1 - heightM / HMAX_M);
        boolean reached = heightM < REACH_M;
        double lost = reached ? share : 0;
        double broom = broomedWinters >= 3 ? 0.5 : 1.0;
        double h = heightM + g * (1 - lost) * broom;
        // the winter's browse also nips the tips of what it reaches: 0.4 m at a share of 1
        return reached ? Math.max(MIN_M, h - DIEBACK_M * share) : h;
    }

    /** Heights after {@code years} years from {@code startM}, fenced or at the given browse share. */
    public static double heightAfter(double startM, int years, double share) {
        double h = startM;
        int run = 0;
        for (int i = 0; i < years; i++) {
            h = growYear(h, share, run);
            run = share > 0.5 && h < REACH_M ? run + 1 : 0;
        }
        return h;
    }
}
