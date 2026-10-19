package com.yearofice.api.env;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.weather.WeatherModel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Layer 2, Environment field (Minecraft development › Layer 2): "How cold, wet, windy, snowy, icy and bright is it
 * here?" has one answer. Body, fire, decay, animals, plants and the HUD ask it and never compute their own.
 *
 * <p>M2 ships the home valley's month normals, hourly curve and daylight table plus a seeded daily draw
 * ({@code weather/interior_alaska.json}); lapse and the winter inversion by height, ridge exposure and canopy for
 * wind, and water temperature from the 5-day mean air. Fronts, storms, the forecast, micro-climates, rooms, held
 * light, ice and the snow model arrive with their systems; until then those fields hold the neutral values
 * {@link EnvSample#outdoors} uses.
 */
public interface EnvService {

    /** Which kind of water a {@code waterC} asks about (Blueprint › Conditions › Water temperature). */
    enum WaterKind { RIVER, CREEK, LAKE }

    /** Wind at a place: direction it blows from (° clockwise from north), mean and gust speed. */
    record Wind(double fromDeg, double ms, double gustMs) {}

    /** One game hour at a place, for closed-form steps ({@code hourlySeries}). */
    record HourAir(long minute, double airC, double feelsLikeC, double windMs) {}

    /**
     * A debug or test override of the valley weather on one level ({@code /yoi env set}); NaN or null keeps the
     * drawn value.
     */
    record WeatherOverride(double airC, double windMs, Precip precip, double precipMmH) {}

    /** The full sample at {@code pos} now. */
    EnvSample sample(ServerLevel level, BlockPos pos);

    /** The sample at {@code pos} at absolute game minute {@code minute}, computed from the year table (no blocks). */
    EnvSample sampleAbstract(ServerLevel level, double x, double y, double z, long minute);

    double airC(ServerLevel level, BlockPos pos);

    double feelsLikeC(ServerLevel level, BlockPos pos);

    /** Ground temperature at {@code depthM} (0.5 m today; deeper reads the same until M4's ground block). */
    double groundC(ServerLevel level, BlockPos pos, double depthM);

    /** Water temperature of {@code kind} now. */
    double waterC(ServerLevel level, BlockPos pos, WaterKind kind);

    /** Water temperature of {@code kind} at absolute minute {@code minute}. */
    double waterCAt(ServerLevel level, WaterKind kind, long minute);

    Wind wind(ServerLevel level, BlockPos pos);

    Precip precip(ServerLevel level, BlockPos pos);

    /** Snow on open ground in cm. */
    double snowCm(ServerLevel level, BlockPos pos);

    double visibilityM(ServerLevel level, BlockPos pos);

    /** Gameplay light 0–15: the larger of sky and block light (held lights arrive with {@code env.light}, M3). */
    int light(ServerLevel level, BlockPos pos);

    /** The valley floor's hour now: the drawn state, air, wind and precipitation before place rules. */
    WeatherModel.Hour valley(ServerLevel level);

    /**
     * The valley floor's hour at absolute game minute {@code minute}, with this level's override, from the year
     * table (no blocks): closed forms that walk the past or the future read it (Decay's {@code air:} clock).
     */
    WeatherModel.Hour valleyAt(ServerLevel level, long minute);

    /** The drawn day {@code absDay}. */
    WeatherModel.Day day(ServerLevel level, long absDay);

    /** Air, feels-like and wind for each game hour from {@code fromDs} to {@code toDs}, from the abstract sample. */
    List<HourAir> hourlySeries(ServerLevel level, BlockPos pos, long fromDs, long toDs);

    /** True when the valley's precipitation this hour is heavy: a Severe day's, or over 4 mm/h. */
    boolean heavy(ServerLevel level);

    void setOverride(ServerLevel level, WeatherOverride override);

    WeatherOverride override(ServerLevel level);

    /**
     * Fixes the weather of May 1 of year 1 for this world (Blueprint › Start site › Day 1 rules), or null to draw it.
     * Unlike {@link #setOverride} it is part of the weather itself: the day chain and the anomaly continue from it.
     * Not saved here: the system that owns the rule (story, for the guided first night) sets it on every start.
     */
    void fixFirstDay(ServerLevel level, WeatherModel.FixedDay day);

    /**
     * A counter that changes whenever already-drawn weather history changes (today: {@link #fixFirstDay}), so
     * caches built from past hours (the decay clock's summed year) know to re-sum. Pins don't count: they are
     * read live by every caller.
     */
    default long historyVersion() { return 0; }

    static EnvService get() {
        EnvService s = Holder.service;
        if (s == null) throw new IllegalStateException("EnvService not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    static void install(EnvService service) {
        if (Holder.service != null) throw new IllegalStateException("EnvService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile EnvService service;

        private Holder() {}
    }
}
