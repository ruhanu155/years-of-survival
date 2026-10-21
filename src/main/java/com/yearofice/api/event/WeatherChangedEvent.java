package com.yearofice.api.event;

import com.yearofice.api.weather.WeatherState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/** Posted by {@code env.weather} when the valley's storm kind or its hourly state changes (checked each game hour and at storm edges). */
public final class WeatherChangedEvent extends Event {
    private final ServerLevel level;
    private final WeatherState from;
    private final WeatherState to;

    public WeatherChangedEvent(ServerLevel level, WeatherState from, WeatherState to) {
        this.level = level;
        this.from = from;
        this.to = to;
    }

    public ServerLevel level() { return level; }

    public WeatherState from() { return from; }

    public WeatherState to() { return to; }
}
