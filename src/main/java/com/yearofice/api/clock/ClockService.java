package com.yearofice.api.clock;

import com.yearofice.sim.time.Rate;
import com.yearofice.sim.time.Sun;
import net.minecraft.server.level.ServerPlayer;

/**
 * Layer 1, Clock and calendar (Minecraft development › Layer 1): the one game clock. Game time is {@code game_ds},
 * game deciseconds since 00:00 on May 1 of year 1; the world's day length (24–72 real minutes) scales only the
 * World rate. Rates are requested per player each tick and the world runs at the slowest player's rate.
 *
 * <p>The docs call this {@code TimeService}; the studio named it {@code ClockService} before it landed, and the
 * methods are the doc's. The clock lives in {@code story.clock}; other systems read time only through here and
 * listen to {@link com.yearofice.api.event.ClockEvent}.
 */
public interface ClockService {

    /** Game deciseconds since 00:00 on May 1 of year 1. */
    long nowDs();

    /** Absolute game minute, 0 at 00:00 on May 1 of year 1. */
    long nowMinute();

    /** Absolute day, 0 on May 1 of year 1. */
    long absDay();

    /** Minute of the day, 0–1,439. */
    int minuteOfDay();

    /** Day of the year, 0–364 (May 1 is 0). */
    int dayOfYear();

    /** Year, 1-based. */
    int year();

    /** The world's rate this tick (the last one stepped). */
    Rate rate();

    /** {@code player}'s own resolved rate on the last tick; World if they asked for nothing. */
    Rate rate(ServerPlayer player);

    /** Game deciseconds a tick at the current world rate. */
    int dsPerTick();

    /** Game deciseconds a tick at {@code rate} on this world's day length. */
    int dsPerTick(Rate rate);

    /** Real minutes per game day at the World rate: 24, 30, 36, 40, 45, 48, 60 or 72. */
    int dayLengthMin();

    /** The daylight table at 64°N. */
    Sun sun();

    /** Sun elevation in degrees now. */
    double sunElevation();

    /** Hours of daylight on absolute day {@code absDay}. */
    double daylightHours(long absDay);

    /** Absolute minute of the dawn of {@code absDay} (the sun climbing through −6°, or the midnight-sun fallback). */
    long dawnMinute(long absDay);

    /** Absolute minute of the dusk of {@code absDay}; may fall after midnight. */
    long duskMinute(long absDay);

    /** Moon phase 0–1 (0 new, 0.5 full), on a 29.53-day cycle from the seeded offset. */
    double moonPhase();

    /** Game minutes since {@code stampMinute} (an absolute minute). */
    default long minutesSince(long stampMinute) { return nowMinute() - stampMinute; }

    /**
     * Asks for {@code rate} for {@code player} this tick. Requests last one tick: a system re-asks every tick it
     * still needs the rate. The clock itself asks Travel for a moving player and Sleep for a sleeping one.
     */
    void requestRate(ServerPlayer player, Rate rate, String reason);

    // ---- admin: debug commands and tests write through these, so they post the same events as play ----------

    /** Moves the clock to day {@code day} (1-based, day 1 = May 1 of year 1) at the same time of day. */
    void setDay(long day);

    /** Sets the clock to {@code gameDs} (tests restore the clock with it); posts {@code ClockEvent.Jump}. */
    void setNow(long gameDs);

    /** Moves the clock on {@code minutes}; over 60 posts {@code ClockEvent.Jump}. */
    void advance(long minutes);

    /** Sets the world's day length (an allowed value) for this world. */
    void setDayLength(int minutes);

    /** Pins the world rate ({@code /yoi time rate}); null returns to arbitration. */
    void forceRate(Rate rate);

    /** The pinned rate, or null. */
    Rate forcedRate();

    static ClockService get() {
        ClockService s = Holder.service;
        if (s == null) throw new IllegalStateException("ClockService not installed yet");
        return s;
    }

    /** True once the clock package installed itself. */
    static boolean installed() { return Holder.service != null; }

    /** Called once by the clock package at mod construction. */
    static void install(ClockService service) {
        if (Holder.service != null) throw new IllegalStateException("ClockService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile ClockService service;

        private Holder() {}
    }
}
