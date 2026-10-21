package com.yearofice.sim.livestock;

/**
 * Livestock needs (Raising animals › Care rules). Fed, watered and warm run 0–100. Below 30 on any the animal stops
 * producing; below 10 it loses Condition; at 0 for 48 game hours it dies. Pure; the entity feeds it once a game hour.
 */
public final class Care {
    private Care() {}

    public static final double PRODUCE_MIN = 30, CONDITION_LOSS_BELOW = 10;
    public static final double CONDITION_LOSS_PER_H = 0.4, CONDITION_GAIN_PER_H = 0.1, DEATH_HOURS = 48;

    public record State(double fed, double watered, double warm, double condition, double zeroHours) {
        public static final State FRESH = new State(100, 100, 100, 100, 0);

        public double lowest() { return Math.min(fed, Math.min(watered, warm)); }

        public boolean producing() { return lowest() >= PRODUCE_MIN; }

        public boolean dead() { return condition <= 0 || zeroHours >= DEATH_HOURS; }
    }

    /**
     * One game hour. {@code fedFraction} and {@code waterFraction} are the share of this hour's ration the trough gave
     * (1 when full, 0 when empty); {@code roomC} is the pen's air temperature (a warm room from the heat model).
     */
    public static State step(State s, Spec sp, LivestockTuning t, double fedFraction, double waterFraction, double roomC) {
        double fd = sp.fedDecayPerDay() / 24 * t.needDecayScale();
        double wd = sp.waterDecayPerDay() / 24 * t.needDecayScale();
        double fed = clamp(s.fed() - fd + fedFraction * (fd + (100 - s.fed()) * 0.25));
        double water = clamp(s.watered() - wd + waterFraction * (wd + (100 - s.watered()) * 0.25));
        double warm = clamp(s.warm() + (warmTarget(sp, roomC) - s.warm()) * 0.15);
        State n = new State(fed, water, warm, s.condition(), s.zeroHours());
        double cond = s.condition();
        if (n.lowest() < CONDITION_LOSS_BELOW) cond -= CONDITION_LOSS_PER_H * t.needDecayScale();
        else if (n.lowest() >= 60) cond += CONDITION_GAIN_PER_H;
        double zero = n.lowest() <= 0.001 ? s.zeroHours() + 1 : 0;
        return new State(fed, water, warm, clamp(cond), zero);
    }

    /** 100 at or above the species' comfort temperature, falling {@code coldSlopePerC} points a degree below it. */
    public static double warmTarget(Spec sp, double roomC) {
        return clamp(100 - Math.max(0, sp.comfortC() - roomC) * sp.coldSlopePerC());
    }

    /** A trough: kilograms of feed (or litres of water) left. */
    public record Trough(double left) {
        public static Trough full(Spec sp, int animals) { return new Trough(sp.feedKgPerDay() * sp.troughFeedDays() * animals); }

        public static Trough fullWater(Spec sp, int animals) { return new Trough(sp.waterLPerDay() * sp.troughWaterDays() * animals); }
    }

    public static double clamp(double v) { return Math.max(0, Math.min(100, v)); }

    // ---- the winter feed debt -----------------------------------------------------------------------------------

    /** Hay an animal still needs to eat between two days of the year (0 = May 1), {@code kgPerDay} each, wrapping the year. */
    public static double hayNeededKg(int animals, double kgPerDay, int fromDoy, int toDoy) {
        int days = toDoy >= fromDoy ? toDoy - fromDoy : 365 - fromDoy + toDoy;
        return animals * kgPerDay * days;
    }

    /** What the store is short: positive is feed owed to the herd, 0 when the stack covers the winter. */
    public static double feedDebtKg(double storeKg, double neededKg) { return Math.max(0, neededKg - storeKg); }

    /** Days the stack feeds the herd. */
    public static double storeDays(double storeKg, int animals, double kgPerDay) { return animals == 0 ? 0 : storeKg / (animals * kgPerDay); }
}
