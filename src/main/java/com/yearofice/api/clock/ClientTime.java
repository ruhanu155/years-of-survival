package com.yearofice.api.clock;

import java.util.function.LongSupplier;

/**
 * The clock as the client knows it: the last {@code clock_sync}, predicted forward each client tick. Common code
 * that also runs on the client (tooltips reading freshness) reads time here on the client side and through
 * {@link ClockService} on the server. The client installs the supplier at start-up.
 */
public final class ClientTime {
    private ClientTime() {}

    public static volatile LongSupplier nowDs = () -> 0L;

    public static double minuteD() { return nowDs.getAsLong() / 600.0; }
}
