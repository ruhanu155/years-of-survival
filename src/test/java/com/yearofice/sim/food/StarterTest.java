package com.yearofice.sim.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** {@code yearofice:food/starter_states} (sim half): the states of the sourdough starter. */
class StarterTest {
    static final long DAY = 1440;

    @Test
    void unfed_at_20c_is_hungry_on_day_4_and_dead_on_day_21() {
        SplittableRandom r = new SplittableRandom(1);
        Starter.Data d = Starter.Data.fresh(0);
        assertEquals(Starter.State.ACTIVE, d.state());
        assertEquals(Starter.State.ACTIVE, Starter.advance(d, 3 * DAY, 20, r).state());
        assertTrue(Starter.advance(d, 3 * DAY, 20, r).feedDue(), "the morning prompt lists 'Feed the starter' at day 3");
        assertEquals(Starter.State.HOOCH, Starter.advance(d, 4 * DAY, 20, r).state());
        assertEquals(Starter.State.HOOCH, Starter.advance(d, 20 * DAY, 20, r).state());
        assertEquals(Starter.State.DEAD, Starter.advance(d, 21 * DAY, 20, r).state());
    }

    @Test
    void at_minus_10c_it_stays_dormant_for_60_days() {
        SplittableRandom r = new SplittableRandom(1);
        Starter.Data d = Starter.Data.fresh(0);
        for (int day = 1; day <= 60; day++) {
            d = Starter.advance(d, DAY, -10, r);
            assertEquals(Starter.State.SLEEPING, d.state(), "day " + day);
        }
        assertEquals(0.0, d.unfedDays(), 1e-9, "the clock stopped");
    }

    @Test
    void two_feeds_revive_a_hungry_starter() {
        SplittableRandom r = new SplittableRandom(1);
        Starter.Data d = Starter.advance(Starter.Data.fresh(0), 10 * DAY, 20, r);
        assertEquals(Starter.State.HOOCH, d.state());
        d = Starter.feed(d, 10 * DAY);
        assertEquals(Starter.State.HOOCH, d.state(), "one feed is not enough");
        d = Starter.feed(d, 10 * DAY + 600);
        assertEquals(Starter.State.ACTIVE, d.state());
        assertEquals(0.0, d.unfedDays(), 1e-9);
        assertEquals(Starter.State.DEAD, Starter.feed(Starter.advance(Starter.Data.fresh(0), 25 * DAY, 20, r), 25 * DAY).state());
    }

    @Test
    void a_feed_every_three_days_keeps_it_active_for_a_year() {
        SplittableRandom r = new SplittableRandom(1);
        Starter.Data d = Starter.Data.fresh(0);
        for (int day = 3; day <= 365; day += 3) {
            d = Starter.advance(d, 3 * DAY, 20, r);
            assertEquals(Starter.State.ACTIVE, d.state());
            d = Starter.feed(d, day * DAY);
        }
        assertEquals(1.0, Starter.riseFactor(Starter.State.ACTIVE), 1e-9);
        assertEquals(0.5, Starter.riseFactor(Starter.State.HOOCH), 1e-9);
    }

    @Test
    void a_freeze_and_thaw_kills_ten_percent_and_thirty_below_minus_30() {
        SplittableRandom r = new SplittableRandom(3);
        int dead = 0, deepDead = 0, n = 4000;
        for (int i = 0; i < n; i++) {
            Starter.Data d = Starter.advance(Starter.Data.fresh(0), DAY, -10, r);
            d = Starter.advance(d, DAY, 10, r);
            if (d.state() == Starter.State.DEAD) dead++;
            else assertEquals(Starter.State.HOOCH, d.state(), "a thawed survivor needs two feeds");
            Starter.Data e = Starter.advance(Starter.Data.fresh(0), DAY, -35, r);
            e = Starter.advance(e, DAY, 10, r);
            if (e.state() == Starter.State.DEAD) deepDead++;
        }
        assertEquals(0.10, dead / (double) n, 0.02);
        assertEquals(0.30, deepDead / (double) n, 0.03);
    }
}
