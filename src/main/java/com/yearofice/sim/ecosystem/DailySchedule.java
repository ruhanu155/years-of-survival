package com.yearofice.sim.ecosystem;

/**
 * Runs {@link Drives} over simulated days for the schedule tests (Blueprint › Drives and daily schedules, Build and
 * test): over 30 July days a moose is bedded for 55–70% of the hours 11:00–16:00 and drinks at dusk on at least 80%
 * of days, and no animal changes act more than 6 times in a game hour.
 *
 * @param bedMiddayShare  share of the steps from 11:00 to 16:00 spent bedded
 * @param duskWaterDays   days with a drink between 17:00 and 21:00
 * @param maxSwitchesHour most act changes in any one game hour
 */
public record DailySchedule(double bedMiddayShare, int duskWaterDays, int days, int maxSwitchesHour, int totalSwitches) {
    public static final int STEPS_PER_HOUR = 6;

    public static DailySchedule run(Drives.Rates r, Drives.Weights w, int days, int warmupDays) {
        Drives.State s = new Drives.State();
        double dt = 1.0 / STEPS_PER_HOUR;
        int bed = 0, mid = 0, water = 0, maxSw = 0, total = 0;
        for (int d = -warmupDays; d < days; d++) {
            boolean drank = false;
            for (int h = 0; h < 24; h++) {
                int before = s.switches;
                for (int k = 0; k < STEPS_PER_HOUR; k++) {
                    Drives.step(s, r, w, h + k * dt, dt);
                    if (d < 0) continue;
                    if (h >= 11 && h < 16) {
                        mid++;
                        if (s.act == Drives.Act.BED) bed++;
                    }
                    if (h >= 17 && h < 21 && s.act == Drives.Act.WATER) drank = true;
                }
                if (d >= 0) {
                    maxSw = Math.max(maxSw, s.switches - before);
                    total += s.switches - before;
                }
            }
            if (d >= 0 && drank) water++;
        }
        return new DailySchedule(mid == 0 ? 0 : bed / (double) mid, water, days, maxSw, total);
    }
}
