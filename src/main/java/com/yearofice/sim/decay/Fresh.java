/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Adapted for Year of Ice from TerraFirmaCraft/TerraFirmaCraft@c3b4d9536b82e5a9746dd793c586720a74108057,
 * src/main/java/net/dries007/tfc/common/component/food/FoodCapability.java, FoodComponent.java and IFood.java
 * (creation date plus trait multiplier, the preserve-the-proportion re-dating derivation, merge by the older
 * date, the never-decays and rotten flags, and sanitise-on-read; reimplemented on our decay-hour axis).
 */
package com.yearofice.sim.decay;

/**
 * The value of the {@code yearofice:fresh} data component (Minecraft development › Layer 5; Items › How a
 * stack reads), as pure data plus the closed-form operations on it.
 *
 * <p><b>What TerraFirmaCraft gives us.</b> TFC stores a creation date and a decay-date modifier; a stack is
 * rotten once {@code now >= creation + modifier × base}. Nothing ticks. Traits multiply the rate, and applying
 * one moves the creation date so the remaining share is unchanged:
 * {@code C_f = (1 − p)·T + p·C_i}. Two stacks merge by taking the older date.
 *
 * <p><b>How it maps onto our rules.</b> Our axis is not world time but the holder's decay clock D, in
 * decay-hours (the prefix sum of f_T, {@link DecayClimate}), so cold time is free and catch-up is two lookups.
 * We store the due point instead of the creation date: {@code p = 1 − (due − D(now)) / life}. TFC's re-dating
 * becomes "scale the remaining decay-hours by the rate change". Moving to another holder keeps the remaining
 * decay-hours on the new clock's axis: {@code due' = D_new(now) + (due − D_old(now))}.
 *
 * <p><b>Bands.</b> {@code due} is floored to a grid of life/40 on the clock's own axis, so stacks that reach the
 * same band on the same clock are equal and merge in vanilla code. Flooring takes the worse value, as Items asks;
 * TFC instead rounds creation dates up (fresher). Every operation that re-dates a stack ages it by at most one
 * band (2.5 freshness points).
 *
 * <p>{@code lifeDh} is the effective life on this holder: the reference life (spoil_days_at_5c × 24 ÷ 0.7, see
 * {@link DecayCore#tauFromSafeLifeDays}) divided by the product of the stack's traits. {@code dueDh} is
 * {@code +∞} for food that never decays (honey, a sealed can) and {@code −∞} once settled rotten.
 *
 * <p>Pure Java, immutable.
 */
public record Fresh(FoodState state, int clock, double dueDh, double lifeDh, int traits, boolean known) {
    /** The band count, {@code [decay] bands} in yearofice-common.toml. */
    public static final int DEFAULT_BANDS = 40;

    public static final double NEVER = Double.POSITIVE_INFINITY;
    public static final double ROTTEN = Double.NEGATIVE_INFINITY;

    public Fresh {
        if (state == null) throw new IllegalArgumentException("state");
        if (!(lifeDh > 0) || Double.isInfinite(lifeDh)) throw new IllegalArgumentException("life must be positive and finite");
        if (Double.isNaN(dueDh)) throw new IllegalArgumentException("due is NaN");
    }

    /** A new stack stamped now on a clock with a reference life and traits. */
    public static Fresh stamp(FoodState state, int clock, double dNow, double refLifeDh, int traits, int bands) {
        double life = refLifeDh / FreshTrait.product(traits);
        return new Fresh(state, clock, quantise(dNow + life, life, bands), life, traits, true);
    }

    public static Fresh stamp(FoodState state, int clock, double dNow, double refLifeDh) {
        return stamp(state, clock, dNow, refLifeDh, 0, DEFAULT_BANDS);
    }

    /** Food that never decays while sealed (canned peaches, honey, pilot bread kept dry). */
    public static Fresh never(FoodState state, int clock, double refLifeDh) {
        return new Fresh(state, clock, NEVER, refLifeDh, 0, true);
    }

    /** Floor {@code due} to the life/bands grid on the clock's axis. Infinite flags pass through. */
    public static double quantise(double due, double life, int bands) {
        if (Double.isInfinite(due)) return due;
        if (bands <= 0) throw new IllegalArgumentException("bands must be positive");
        double band = life / bands;
        double k = StrictMath.floor(due / band);
        // Division can land a hair under an exact multiple; correct so quantising is idempotent.
        if ((k + 1) * band <= due) k += 1;
        else if (k * band > due) k -= 1;
        return k * band;
    }

    public boolean neverDecays() { return dueDh == NEVER; }

    /** Decay progress p at clock reading {@code dNow}, in [0, 1]. */
    public double progress(double dNow) {
        if (dueDh == NEVER) return 0.0;
        if (dueDh == ROTTEN) return 1.0;
        return DecayLadder.clamp01(1.0 - (dueDh - dNow) / lifeDh);
    }

    public double freshness(double dNow) { return DecayLadder.freshness(progress(dNow)); }

    public int stage(double dNow) { return DecayLadder.FOOD.stageOf(progress(dNow)); }

    public boolean rotten(double dNow) { return progress(dNow) >= 1.0; }

    /** Decay-hours left until p reaches {@code target} on this clock; 0 if already past, ∞ if never. */
    public double decayHoursUntil(double target, double dNow) {
        if (dueDh == NEVER) return Double.POSITIVE_INFINITY;
        double atTarget = dueDh - (1.0 - target) * lifeDh;
        return Math.max(0.0, atTarget - dNow);
    }

    /** Lazy settling on read (TFC's sanitise): a stack past p = 1 is flagged rotten so it stays rotten. */
    public Fresh settle(double dNow) {
        if (dueDh != NEVER && dueDh != ROTTEN && rotten(dNow)) {
            return new Fresh(state, clock, ROTTEN, lifeDh, traits, known);
        }
        return this;
    }

    /** Move to another holder's clock, keeping the remaining decay-hours, then floor to the band grid. */
    public Fresh rehome(int newClock, double dOldNow, double dNewNow, int bands) {
        if (Double.isInfinite(dueDh)) return new Fresh(state, newClock, dueDh, lifeDh, traits, known);
        double due = dNewNow + (dueDh - dOldNow);
        return new Fresh(state, newClock, quantise(due, lifeDh, bands), lifeDh, traits, known);
    }

    /**
     * Apply a trait, keeping p (TFC applyTrait). The remaining decay-hours and the life both scale by 1 ÷ rate.
     * Rotten food takes no trait, as in TFC.
     */
    public Fresh withTrait(FreshTrait t, double dNow, int bands) {
        if (FreshTrait.has(traits, t) || rotten(dNow)) return this;
        return retrait(traits | t.bit(), dNow, bands);
    }

    /** Remove a trait, keeping p (TFC removeTrait). */
    public Fresh withoutTrait(FreshTrait t, double dNow, int bands) {
        if (!FreshTrait.has(traits, t)) return this;
        return retrait(traits & ~t.bit(), dNow, bands);
    }

    private Fresh retrait(int newTraits, double dNow, int bands) {
        double scale = FreshTrait.product(traits) / FreshTrait.product(newTraits);
        double life = lifeDh * scale;
        if (Double.isInfinite(dueDh)) return new Fresh(state, clock, dueDh, life, newTraits, known);
        double remaining = Math.max(0.0, dueDh - dNow) * scale;
        return new Fresh(state, clock, quantise(dNow + remaining, life, bands), life, newTraits, known);
    }

    /**
     * Change state, as cooking or smoking does.
     *
     * @param keepProgress true to keep p, as TFC's updateFoodFromPrevious does (cooking raw meat keeps its age);
     *                     false to start the new life fresh, for a new product (dried strips, smoked fish)
     * @param dOldNow      this clock's reading now, on the old state's curve
     * @param newClock     the clock handle for the new state's curve (fixed-life curves have their own axis)
     * @param dNewNow      that clock's reading now
     */
    public Fresh withState(FoodState newState, double newRefLifeDh, boolean keepProgress,
                           double dOldNow, int newClock, double dNewNow, int bands) {
        double life = newRefLifeDh / FreshTrait.product(traits);
        if (dueDh == NEVER) return new Fresh(newState, newClock, NEVER, life, traits, known);
        double p = keepProgress ? progress(dOldNow) : 0.0;
        double due = dNewNow + (1.0 - p) * life;
        return new Fresh(newState, newClock, quantise(due, life, bands), life, traits, known);
    }

    /** Found food reads "Unknown freshness" until sniffed or cut. */
    public Fresh withKnown(boolean k) { return new Fresh(state, clock, dueDh, lifeDh, traits, k); }

    /**
     * True if two stacks would be equal but for their due point: same state, clock, life, traits and known flag
     * (TFC areStacksStackableExceptCreationDate; different traits never stack).
     */
    public boolean mergeableWith(Fresh o) {
        return state == o.state && clock == o.clock && lifeDh == o.lifeDh && traits == o.traits && known == o.known;
    }

    /** Merge two stacks: the result takes the worse (earlier) due point, as TFC takes the older date. */
    public static Fresh merge(Fresh a, Fresh b) {
        if (!a.mergeableWith(b)) throw new IllegalArgumentException("stacks differ in more than their due point");
        return a.dueDh <= b.dueDh ? a : b;
    }
}
