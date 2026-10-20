package com.yearofice.sim.fire;

import java.util.HashMap;
import java.util.Map;

/**
 * The slip guard for putting a fire out by accident (Every concept from the video › Fire mishaps and slips of the
 * hand): the first time each game day, pouring water on a burning fire needs a deliberate second action within a few
 * seconds and shows "Hold to put out the fire."; later the same day it is instant, "because by then the player means
 * it". A fire already at coals or banked is never guarded. The server decides, so a modified client cannot skip it;
 * the client's hold ring is a nicety on top.
 *
 * @param <K> who is asking (a player's UUID)
 */
public final class SmotherGuard<K> {
    /** How long the first pour stays armed, in server ticks (3 s). */
    public static final long ARM_TICKS = 60;

    private final Map<K, Long> armedUntil = new HashMap<>();
    private final Map<K, Long> doneDay = new HashMap<>();

    /**
     * True when the pour goes ahead now. {@code day} is the absolute game day, {@code tick} the server tick.
     * {@code guarded} is false for coals and banked coals. A false return has armed the guard: the same player's
     * next call within {@link #ARM_TICKS} goes ahead.
     */
    public boolean allow(K who, long day, long tick, boolean guarded) {
        if (!guarded) return true;
        Long done = doneDay.get(who);
        if (done != null && done == day) return true;
        Long armed = armedUntil.get(who);
        if (armed != null && tick <= armed) {
            doneDay.put(who, day);
            armedUntil.remove(who);
            return true;
        }
        armedUntil.put(who, tick + ARM_TICKS);
        return false;
    }

    public void clear() {
        armedUntil.clear();
        doneDay.clear();
    }
}
