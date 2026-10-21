package com.yearofice.sim.body;

/** A body step that holds both sleeping and awake ticks is split, so a short wake does not turn the sleep awake. */
public final class SleepSplit {
    private SleepSplit() {}

    /** Minutes of {@code minutes} to credit as sleep when {@code sleepTicks} of {@code ticks} sampled ticks were asleep. */
    public static int sleepMinutes(int minutes, int sleepTicks, int ticks) {
        if (ticks <= 0 || sleepTicks <= 0) return 0;
        if (sleepTicks >= ticks) return minutes;
        int m = (int) Math.round((double) minutes * sleepTicks / ticks);
        return minutes >= 2 ? Math.max(1, Math.min(minutes - 1, m)) : (m > 0 ? minutes : 0);
    }
}
