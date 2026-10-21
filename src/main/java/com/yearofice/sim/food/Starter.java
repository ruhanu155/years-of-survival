package com.yearofice.sim.food;

import java.util.random.RandomGenerator;

/**
 * The sourdough starter (B6; Items › Food values). A living thing kept by feeding: fed within 3 days it is Active; unfed
 * 4 to 20 days above 5 °C it is Hungry (the {@code hooch} state: sour, rising half, two feeds revive it); below 0 °C the
 * clock stops and it sleeps; unfed 21 days at room warmth it is Dead. A freeze and thaw kills it 10% of the time (30%
 * below −30 °C). Pure; {@code unfedDays} counts warm days, so a cold week costs nothing and a cool one costs part.
 */
public final class Starter {
    private Starter() {}

    public enum State { ACTIVE, HOOCH, SLEEPING, DEAD }

    public static final double FEED_DAYS = 3, HUNGRY_DAYS = 4, DEAD_DAYS = 21, FREEZE_DEATH = 0.10, DEEP_FREEZE_DEATH = 0.30;
    public static final int REVIVE_FEEDS = 2;
    public static final double WARM_FULL_C = 20, CLOCK_RUNS_ABOVE_C = 5, SLEEPS_BELOW_C = 0, FROZEN_BELOW_C = -5;

    /**
     * @param hydrationPct 100 for the usual flour-and-water paste
     * @param unfedDays    warm-equivalent days since the last full feed
     * @param reviveFeeds  feeds still owed before a Hungry or thawed starter is Active again
     * @param frozen       it has been below −5 °C since it last thawed
     * @param deepFrozen   it has been below −30 °C
     * @param lastFedMinute the absolute minute of the last feed, for the tooltip ("Fed 2 days ago")
     */
    public record Data(double hydrationPct, double unfedDays, int reviveFeeds, boolean frozen, boolean deepFrozen, long lastFedMinute,
                       boolean cold, boolean dead) {
        public static Data fresh(long minute) { return new Data(100, 0, 0, false, false, minute, false, false); }

        public State state() {
            if (dead || unfedDays >= DEAD_DAYS) return State.DEAD;
            if (cold) return State.SLEEPING;
            if (reviveFeeds > 0 || unfedDays >= HUNGRY_DAYS) return State.HOOCH;
            return State.ACTIVE;
        }

        /** A feed is due once it has gone the feed interval. */
        public boolean feedDue() { return state() != State.DEAD && unfedDays >= FEED_DAYS; }
    }

    /** How fast the unfed clock runs at an air temperature: 0 at or below 5 °C, 1 at 20 °C and above. */
    public static double clockRate(double tempC) { return Math.max(0, Math.min(1, (tempC - CLOCK_RUNS_ABOVE_C) / (WARM_FULL_C - CLOCK_RUNS_ABOVE_C))); }

    /** Lets {@code minutes} pass at {@code tempC}. A thaw (back above 0 °C after a freeze) rolls the freeze death once. */
    public static Data advance(Data d, double minutes, double tempC, RandomGenerator rng) {
        if (d.dead()) return d;
        boolean cold = tempC < SLEEPS_BELOW_C;
        boolean frozen = d.frozen() || tempC < FROZEN_BELOW_C;
        boolean deep = d.deepFrozen() || tempC < -30;
        int owed = d.reviveFeeds();
        boolean dead = false;
        if (!cold && d.frozen()) {
            // thawed: one roll, and two feeds are owed after a thaw
            dead = rng.nextDouble() < (d.deepFrozen() ? DEEP_FREEZE_DEATH : FREEZE_DEATH);
            frozen = false;
            deep = false;
            owed = Math.max(owed, REVIVE_FEEDS);
        }
        double unfed = d.unfedDays() + (cold ? 0 : clockRate(tempC) * minutes / 1440.0);
        return new Data(d.hydrationPct(), unfed, owed, frozen, deep, d.lastFedMinute(), cold, dead);
    }

    /**
     * One feed of 50 g flour. An Active starter resets its clock; a Hungry or thawed one counts down the feeds it owes
     * and is Active again when they are paid. A Dead starter stays dead.
     */
    public static Data feed(Data d, long minute) {
        if (d.state() == State.DEAD) return d;
        if (d.state() == State.ACTIVE) return new Data(100, 0, 0, d.frozen(), d.deepFrozen(), minute, d.cold(), false);
        int owed = d.reviveFeeds() > 0 ? d.reviveFeeds() - 1 : d.state() == State.HOOCH ? REVIVE_FEEDS - 1 : 0;
        boolean revived = owed == 0;
        return new Data(100, revived ? 0 : d.unfedDays(), owed, d.frozen(), d.deepFrozen(), minute, d.cold(), false);
    }

    /** What the bread does: Active gives a full rise, Hungry half, anything else none. */
    public static double riseFactor(State s) { return s == State.ACTIVE ? 1.0 : s == State.HOOCH ? 0.5 : 0.0; }
}
