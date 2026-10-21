package com.yearofice.api.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Storm weather (Weather and storms). The day chain and the hour's numbers are {@code EnvService}'s; this adds the storm on top
 * and is the one reader for blocks and ecology that need "is it glazing, hailing, blizzarding here". The owner is
 * {@code env.weather}; read-only for everyone else.
 */
public interface WeatherService {
    /** The storm weather at {@code pos} now. */
    WeatherState current(ServerLevel level, BlockPos pos);

    /** The storm weather at {@code pos} at absolute game minute {@code minute}, from the year table. */
    WeatherState at(ServerLevel level, BlockPos pos, long minute);

    static WeatherService get() {
        WeatherService s = Holder.service;
        if (s == null) throw new IllegalStateException("WeatherService not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    static void install(WeatherService s) { Holder.service = s; }

    final class Holder {
        private static volatile WeatherService service;

        private Holder() {}
    }
}
