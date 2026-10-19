package com.yearofice.sim.time;

import java.util.Arrays;

/**
 * The rates of {@code data/yearofice/clock/rates.json}, in whole game deciseconds per server tick (Layer 1:
 * "Integer game time"). Only the World rate scales with the world's day length L: it is
 * {@code worldDsAt24 × 24 ÷ L}, exact for every allowed L, so no rounding ever drifts (RECONCILE 117, 145).
 *
 * @param closeDs     Close, 3
 * @param travelDs    Travel, 6
 * @param worldDsAt24 World on the 24-minute day, 30
 * @param workDs      Work, 150
 * @param untilDoneDs Until done, 600
 * @param sleepDs     Sleep and wait, 1,800
 * @param dayLengths  the allowed day lengths in real minutes: 24, 30, 36, 40, 45, 48, 60, 72
 * @param dayOnePct   Day 1, the World rate's share while a guided-night player has no fire yet, percent: 50
 *                    (Blueprint › Start site › Day 1 rules, "the clock runs at half speed until the first fire")
 */
public record Rates(int closeDs, int travelDs, int worldDsAt24, int workDs, int untilDoneDs, int sleepDs, int[] dayLengths,
                    int dayOnePct) {

    /** Day 1's share of the World rate when {@code clock/rates.json} names none: half. */
    public static final int DEFAULT_DAY_ONE_PCT = 50;

    public static final Rates STANDARD = new Rates(3, 6, 30, 150, 600, 1800, new int[] {24, 30, 36, 40, 45, 48, 60, 72});

    /** The six rates with Day 1 at half the World rate. */
    public Rates(int closeDs, int travelDs, int worldDsAt24, int workDs, int untilDoneDs, int sleepDs, int[] dayLengths) {
        this(closeDs, travelDs, worldDsAt24, workDs, untilDoneDs, sleepDs, dayLengths, DEFAULT_DAY_ONE_PCT);
    }

    public Rates {
        if (closeDs <= 0 || travelDs <= 0 || worldDsAt24 <= 0 || workDs <= 0 || untilDoneDs <= 0 || sleepDs <= 0) {
            throw new IllegalArgumentException("every rate must be positive");
        }
        if (dayOnePct < 1 || dayOnePct > 100) throw new IllegalArgumentException("first_fire_world_pct is 1-100");
        dayLengths = dayLengths.clone();
        for (int l : dayLengths) {
            if (l <= 0 || (worldDsAt24 * 24) % l != 0) {
                throw new IllegalArgumentException("day length " + l + " min does not divide the World rate exactly");
            }
        }
    }

    /** True when {@code minutes} is one of the allowed day lengths. */
    public boolean allowed(int minutes) { return Arrays.stream(dayLengths).anyMatch(l -> l == minutes); }

    /** The allowed day length closest to {@code minutes}, ties to the shorter. */
    public int nearestAllowed(int minutes) {
        int best = dayLengths[0];
        for (int l : dayLengths) if (Math.abs(l - minutes) < Math.abs(best - minutes)) best = l;
        return best;
    }

    /** Game deciseconds a tick at {@code rate} on a day of {@code dayLengthMin} real minutes. */
    public int dsPerTick(Rate rate, int dayLengthMin) {
        return switch (rate) {
            case CLOSE -> closeDs;
            case TRAVEL -> travelDs;
            case WORLD -> worldDsAt24 * 24 / dayLengthMin;
            case DAY_ONE -> worldDsAt24 * 24 / dayLengthMin * dayOnePct / 100;
            case WORK -> workDs;
            case UNTIL_DONE -> untilDoneDs;
            case SLEEP -> sleepDs;
        };
    }

    /**
     * Hundredths of a game decisecond a tick at {@code rate}. Every rate but Day 1 is a whole number of ds; Day 1
     * is the World rate times its percent, which on the 48-minute day is 7.5 ds a tick. The clock carries the
     * remainder from tick to tick, so Day 1 time stays exact over any run of ticks (no drift, Layer 1's integer rule).
     */
    public int centiDsPerTick(Rate rate, int dayLengthMin) {
        if (rate == Rate.DAY_ONE) return worldDsAt24 * 24 / dayLengthMin * dayOnePct;
        return dsPerTick(rate, dayLengthMin) * 100;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Rates r && r.closeDs == closeDs && r.travelDs == travelDs && r.worldDsAt24 == worldDsAt24
                && r.workDs == workDs && r.untilDoneDs == untilDoneDs && r.sleepDs == sleepDs
                && Arrays.equals(r.dayLengths, dayLengths);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(new int[] {closeDs, travelDs, worldDsAt24, workDs, untilDoneDs, sleepDs, dayOnePct}) * 31 + Arrays.hashCode(dayLengths); }

    @Override
    public int[] dayLengths() { return dayLengths.clone(); }
}
