package com.yearofice.api.event;

import com.yearofice.sim.time.Rate;
import net.neoforged.bus.api.Event;

/** The world rate changed (Events and hooks): client sync and ambience listen. */
public final class RateChangedEvent extends Event {
    private final Rate from;
    private final Rate to;

    public RateChangedEvent(Rate from, Rate to) {
        this.from = from;
        this.to = to;
    }

    public Rate from() { return from; }

    public Rate to() { return to; }
}
