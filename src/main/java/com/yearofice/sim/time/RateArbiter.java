package com.yearofice.sim.time;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@code RateArbiter} (Layer 1, "Choosing the rate"): each player's requests resolve by priority (close, work,
 * until done, sleep, travel, world); the world then runs at the slowest player's rate (the Multiplayer rule,
 * RECONCILE 115), so in single player the world follows the one player.
 */
public final class RateArbiter {
    private RateArbiter() {}

    /** One player's rate from the set of rates requested for them this tick; none requested is World. */
    public static Rate forPlayer(Set<Rate> requested) {
        for (Rate r : Rate.values()) if (requested.contains(r)) return r;
        return Rate.WORLD;
    }

    /** The world's rate: the slowest of the players' rates; with nobody to ask, World. */
    public static Rate forWorld(Collection<Rate> perPlayer, Rates rates, int dayLengthMin) {
        Rate best = null;
        for (Rate r : perPlayer) {
            if (best == null || rates.dsPerTick(r, dayLengthMin) < rates.dsPerTick(best, dayLengthMin)) best = r;
        }
        return best == null ? Rate.WORLD : best;
    }

    public static Set<Rate> none() { return EnumSet.noneOf(Rate.class); }
}
