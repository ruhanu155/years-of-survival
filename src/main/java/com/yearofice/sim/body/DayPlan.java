package com.yearofice.sim.body;

/**
 * A scripted day: one activity per hour of the day, for fast-forwarding a body without a player driving it
 * ({@code /yoi time forward <minutes> chores}, the starvation and day-balance tests).
 *
 * <p>{@link #CHORES} is the slice's "scripted day of chores" (M2.5 test {@code body/slice_day_balance}: 2,600–3,200
 * kcal at the default body): 8 h asleep (22:00–06:00), 11 h resting or by the fire, 3 h walking and 2 h of heavy
 * work. At BMR 1,794 that is 480 + 825 + 750 + 900 = 2,955 kcal.
 */
public record DayPlan(String id, Activity[] hours) {
    public DayPlan {
        if (hours.length != 24) throw new IllegalArgumentException("24 hours");
        hours = hours.clone();
    }

    public Activity at(long minute) { return hours[(int) Math.floorMod(Math.floorDiv(minute, 60), 24)]; }

    private static final Activity S = Activity.SLEEP, R = Activity.REST, W = Activity.WALK, H = Activity.HEAVY;

    public static final DayPlan CHORES = new DayPlan("chores", new Activity[] {
            S, S, S, S, S, S,   // 00–06
            R, R, W, W, H, R,   // 06–12
            R, W, H, R, R, R,   // 12–18
            R, R, R, R, S, S}); // 18–24

    public static final DayPlan STILL = new DayPlan("still", new Activity[] {
            S, S, S, S, S, S, R, R, R, R, R, R, R, R, R, R, R, R, R, R, R, R, S, S});

    /** kcal the plan burns in one day at the default body and the given burn scale, with no shivering. */
    public double kcalPerDay(double burnScale) {
        double sum = 0;
        for (Activity a : hours) sum += Metabolism.kcalPerHour(a) * burnScale;
        return sum;
    }

    /**
     * Steps {@code minutes} through the plan, one plan hour at a time (each hour is one closed-form chunk).
     */
    public BodyModel.Result run(BodyState s, BodyModel.Inputs base, BodyTuning t, long start, int minutes) {
        long m = start;
        long end = start + minutes;
        while (m < end) {
            int n = (int) Math.min(end - m, 60 - Math.floorMod(m, 60));
            BodyModel.Result r = BodyModel.step(s, base.doing(at(m)), t, m, n);
            if (r.collapsed()) return r;
            m += n;
        }
        return BodyModel.Result.NONE;
    }
}
