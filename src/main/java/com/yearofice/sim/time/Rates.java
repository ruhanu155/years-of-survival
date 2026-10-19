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
 */
public record Rates(int closeDs, int travelDs, int worldDsAt24, int workDs, int untilDoneDs, int sleepDs, int[] dayLengths) {

    public static final Rates STANDARD = new Rates(3, 6, 30, 150, 600, 1800, new int[] {24, 30, 36, 40, 45, 48, 60, 72});

    public Rates {
        if (closeDs <= 0 || travelDs <= 0 || worldDsAt24 <= 0 || workDs <= 0 || untilDoneDs <= 0 || sleepDs <= 0) {
            throw new IllegalArgumentException("every rate must be positive");
        }
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
            case WORK -> workDs;
            case UNTIL_DONE -> untilDoneDs;
            case SLEEP -> sleepDs;
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Rates r && r.closeDs == closeDs && r.travelDs == travelDs && r.worldDsAt24 == worldDsAt24
                && r.workDs == workDs && r.untilDoneDs == untilDoneDs && r.sleepDs == sleepDs
                && Arrays.equals(r.dayLengths, dayLengths);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(new int[] {closeDs, travelDs, worldDsAt24, workDs, untilDoneDs, sleepDs}) * 31 + Arrays.hashCode(dayLengths); }

    @Override
    public int[] dayLengths() { return dayLengths.clone(); }
}
