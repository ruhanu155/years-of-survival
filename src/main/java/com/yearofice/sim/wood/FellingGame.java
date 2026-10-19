package com.yearofice.sim.wood;

/**
 * The felling game for one tree, M2 slice form (Trees, felling and firewood › Felling, step by step): the first
 * swing locks the fall arrow toward the feller; the first third of the swings cut the notch on the arrow side,
 * and the rest are the back cut from the far side. The tree falls along the arrow, toward the notch.
 *
 * <p>Where a swing lands is read from where the feller stands (the bearing from the trunk to the player), since
 * the M2 slice has no notch reticle yet:
 * <ul>
 *   <li>notch: within 45° of the arrow counts 1; within 90° counts half (off the marked notch); from the far half
 *       it counts nothing (the back cut before the notch is done, whose barber chair is an M6 hazard);</li>
 *   <li>back cut: within 45° of the far side counts 1; within 90° counts half; from the notch half nothing.</li>
 * </ul>
 * Turning the arrow with Alt and the wheel, lean, wind and the mishaps join in M6.
 */
public final class FellingGame {
    public enum Strike { NOTCH, NOTCH_HALF, BACK, BACK_HALF, NOTCH_FIRST, WRONG_SIDE }

    private final int swings;
    private final int notchNeeded;
    private double arrow = Double.NaN;
    private double notch;
    private double back;
    private int struck;

    public FellingGame(int swings) {
        this.swings = Math.max(1, swings);
        this.notchNeeded = Felling.notchSwings(this.swings);
    }

    public int swings() { return swings; }

    public int notchNeeded() { return notchNeeded; }

    public int backNeeded() { return swings - notchNeeded; }

    public double notch() { return notch; }

    public double back() { return back; }

    /** Swings struck, whether they counted or not. */
    public int struck() { return struck; }

    public boolean arrowLocked() { return !Double.isNaN(arrow); }

    /** The fall arrow's compass bearing, or NaN before the first swing. */
    public double arrow() { return arrow; }

    /** Locks the arrow before the first swing (the debug command fells along a given bearing). */
    public void lockArrow(double bearing) { if (!arrowLocked()) arrow = ((bearing % 360) + 360) % 360; }

    public boolean notchDone() { return notch >= notchNeeded - 1e-9; }

    public boolean done() { return notchDone() && back >= backNeeded() - 1e-9; }

    /** Whether the next counting swing is the last one (the creak plays as it starts). */
    public boolean lastSwingNext() {
        if (!notchDone()) return backNeeded() <= 0 && notch + 1 >= notchNeeded - 1e-9;
        return back + 1 >= backNeeded() - 1e-9;
    }

    /** One swing from a player standing at compass bearing {@code from} as seen from the trunk. */
    public Strike strike(double from) {
        struck++;
        if (!arrowLocked()) arrow = from;
        double a = Felling.angleBetween(from, arrow);
        if (!notchDone()) {
            if (a <= 45) { notch += 1; return Strike.NOTCH; }
            if (a <= 90) { notch += 0.5; return Strike.NOTCH_HALF; }
            return Strike.NOTCH_FIRST;
        }
        if (a >= 135) { back += 1; return Strike.BACK; }
        if (a >= 90) { back += 0.5; return Strike.BACK_HALF; }
        return Strike.WRONG_SIDE;
    }

    /** The share of the work done, 0–1 (counting swings over swings needed). */
    public double share() { return Math.min(1, (Math.min(notch, notchNeeded) + Math.min(back, backNeeded())) / swings); }

    /** The direction the tree falls: along the arrow, toward the notch. */
    public double fallBearing() { return arrow; }
}
