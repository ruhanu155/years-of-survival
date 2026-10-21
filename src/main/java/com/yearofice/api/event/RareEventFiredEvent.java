package com.yearofice.api.event;

import net.neoforged.bus.api.Event;

/** A rare event rolled true for the year and its window opened today (story.rare.RareEventTicker; ids from data event/&lt;id&gt;.json). */
public final class RareEventFiredEvent extends Event {
    private final String id;
    private final int year;
    private final long absDay;

    public RareEventFiredEvent(String id, int year, long absDay) { this.id = id; this.year = year; this.absDay = absDay; }

    public String id() { return id; }
    public int year() { return year; }
    public long absDay() { return absDay; }
}
