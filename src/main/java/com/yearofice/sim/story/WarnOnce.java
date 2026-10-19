package com.yearofice.sim.story;

import java.util.Map;
import java.util.TreeMap;

/**
 * Blueprint › Moment-to-moment mechanics › Warn once: every punished mistake has a warning id. It fires the first
 * time its condition becomes true, and fires again only after the condition has been false for 24 game hours.
 * Warnings never block an action. One instance per player; plain data, saved by the story attachment.
 */
public final class WarnOnce {
    /** Game minutes the condition must stay false before a warning re-arms. */
    public static final long REARM_MIN = 24 * 60;

    /** One id's memory: whether it has fired, and since when its condition has been false (−1 while true). */
    public record Entry(boolean fired, long falseSince) {}

    private final Map<String, Entry> entries = new TreeMap<>();

    /**
     * Feeds the condition's value now. Returns true exactly when the warning should be posted at this minute.
     */
    public boolean update(String id, boolean condition, long minute) {
        Entry e = entries.get(id);
        if (condition) {
            if (e == null || !e.fired()) {
                entries.put(id, new Entry(true, -1));
                return true;
            }
            if (e.falseSince() >= 0) entries.put(id, new Entry(true, -1));
            return false;
        }
        if (e == null || !e.fired()) return false;
        if (e.falseSince() < 0) {
            entries.put(id, new Entry(true, minute));
        } else if (minute - e.falseSince() >= REARM_MIN) {
            entries.put(id, new Entry(false, -1));
        }
        return false;
    }

    public boolean fired(String id) {
        Entry e = entries.get(id);
        return e != null && e.fired();
    }

    /** A copy of the entries, sorted by id, for saving. */
    public Map<String, Entry> entries() { return new TreeMap<>(entries); }

    public void restore(Map<String, Entry> saved) {
        entries.clear();
        entries.putAll(saved);
    }

    public void clear() { entries.clear(); }
}
