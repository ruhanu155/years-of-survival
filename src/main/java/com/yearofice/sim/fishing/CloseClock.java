package com.yearofice.sim.fishing;

/**
 * The close clock as the fight uses it (Blueprint › Fishing methods › The fight, as built: "From the strike until
 * the fish is landed or lost, the game clock runs at a tenth of its speed for that player"; Moment-to-moment
 * mechanics › Four time rates). Each server tick the world clock would add one tick; on the close clock it adds
 * {@code rate} instead, carried as a fraction so ten ticks at 0.1 add exactly one.
 *
 * <p>Pure Java, immutable. Original Year of Ice code.
 */
public record CloseClock(double carry) {
    public static final CloseClock ZERO = new CloseClock(0);

    /** One tick at {@code rate}: the whole ticks the world clock may move now, and the carry left over. */
    public Step tick(double rate) {
        double c = carry + Math.max(0.0, Math.min(1.0, rate));
        long whole = (long) Math.floor(c + 1e-9);
        return new Step(whole, new CloseClock(Math.max(0.0, c - whole)));
    }

    public record Step(long ticks, CloseClock next) {}
}
