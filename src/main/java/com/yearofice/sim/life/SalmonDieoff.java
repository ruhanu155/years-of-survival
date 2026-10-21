package com.yearofice.sim.life;

/**
 * Spawned-out salmon (Waters and fish › The water calendar: "10 days after each run's peak to freeze-up"): after the
 * peak plus {@code dieoffDays} the fish that spawned are dead, drifting and lodging as carcasses that feed the valley
 * (gulls, eagles, ravens, bears). Pure Java. Original Year of Ice code.
 */
public final class SalmonDieoff {
    private SalmonDieoff() {}

    /** True from {@code dieoffDays} after the run's peak until the end of the year's carcass season. */
    public static boolean dying(int doy, int peakDoy, int freezeUpDoy, double dieoffDays) {
        return doy >= peakDoy + dieoffDays && doy <= freezeUpDoy;
    }

    /** Spent bodies to show for a run: its size scaled to the visible fish, never more than {@code cap}. */
    public static int bodies(double runSize, double visibleShare, int cap) {
        return (int) Math.max(0, Math.min(cap, Math.round(runSize * visibleShare)));
    }

    /** What a rotting salmon gives the valley a day, in kg of scavenger food: whole carcass until it is gone. */
    public static double foodKgLeft(double wholeKg, double ageDays, double eatenKgPerDay) {
        return Math.max(0, wholeKg - ageDays * eatenKgPerDay);
    }
}
