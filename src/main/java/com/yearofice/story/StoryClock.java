package com.yearofice.story;

import com.yearofice.api.clock.ClockService;
import java.util.function.LongUnaryOperator;
import net.minecraft.server.MinecraftServer;

/**
 * The onboarding's reading of Layer 1: the absolute game minute and the sun's times, all from
 * {@link ClockService} (the daylight table at 64°N). Sunset is the sun at 0° (the dusk warning's anchor, as the
 * Blueprint's "Sunset 21:45" is); dawn is the sun climbing through −6°, the moment {@code ClockEvent.Dawn} posts.
 */
public final class StoryClock {
    private StoryClock() {}

    /** The absolute game minute now (minute 0 is 00:00 on May 1 of year 1). */
    public static long minute(MinecraftServer server) { return ClockService.get().nowMinute(); }

    /** Absolute day to the absolute minute of its sunset. */
    public static final LongUnaryOperator SUNSET = d -> d * 1440L + ClockService.get().sun().sunsetMinute(d);

    /** Absolute day to the absolute minute of its dawn. */
    public static final LongUnaryOperator DAWN = d -> ClockService.get().dawnMinute(d);

    /** Sunset on the day of {@code minute}. */
    public static long sunsetOf(long minute) { return SUNSET.applyAsLong(Math.floorDiv(minute, 1440L)); }
}
