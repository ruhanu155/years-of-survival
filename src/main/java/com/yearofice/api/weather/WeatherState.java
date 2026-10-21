package com.yearofice.api.weather;

import com.yearofice.sim.weather.StormKind;

/** The storm weather at a place and hour; {@code kind} is CLEAR when no storm is up. {@code precipKind} may be NONE. */
public record WeatherState(StormKind kind, double intensity, double windMs, double fromDeg, double precipMmPerH,
                           StormKind.PrecipKind precipKind, double visibilityM) {}
