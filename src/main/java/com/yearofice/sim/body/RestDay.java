package com.yearofice.sim.body;

/**
 * Rest days (Every concept from the video › Rest days). A game day in which the player stays within 10 m of a warm
 * fire or in a warm room, works under 1 game hour and sleeps 8 hours is a Rest day; healing runs at 2× on it.
 * Counters reset at 00:00. While the day is still unbroken (work ≤ 60, away ≤ 30) healing already doubles; the
 * sleep test is final only when the day closes, which feeds the run ({@code rest_run}).
 */
public final class RestDay {
    private RestDay() {}

    public static final int WORK_MAX_MIN = 60, AWAY_MAX_MIN = 30, SLEEP_MIN = 480;

    public static boolean unbroken(BodyState s) { return s.restWorkMin <= WORK_MAX_MIN && s.restAwayMin <= AWAY_MAX_MIN; }

    /** Work minutes an activity adds per minute: heavy work counts whole, a walk half (light jobs count half). */
    public static double workPerMinute(Activity a) {
        return switch (a) {
            case HEAVY, RUN, SWIM -> 1.0;
            case WALK -> 0.5;
            default -> 0.0;
        };
    }

    /** Called once per chunk. {@code nearWarm}: within 10 m of a warm fire or in a warm room. */
    public static void tick(BodyState s, Activity a, boolean asleep, boolean nearWarm, long m, int n) {
        long day = Math.floorDiv(m, 1440);
        if (s.restDay < 0) s.restDay = day;
        while (s.restDay < day) {
            boolean good = unbroken(s) && s.restSleepMin >= SLEEP_MIN;
            s.restRun = good ? s.restRun + 1 : 0;
            IllnessModel.event(s, good ? "rest_day" : "rest_broken");
            s.restWorkMin = 0;
            s.restAwayMin = 0;
            s.restSleepMin = 0;
            s.restDay++;
        }
        s.restWorkMin += workPerMinute(a) * n;
        if (!nearWarm) s.restAwayMin += n;
        if (asleep) s.restSleepMin += n;
    }
}
