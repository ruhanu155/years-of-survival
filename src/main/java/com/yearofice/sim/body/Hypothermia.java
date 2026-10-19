package com.yearofice.sim.body;

/**
 * The warmth stages and their effects (Blueprint › Warmth › Stages). Lower bounds are inclusive:
 * 70 is Warm, 50 is Cool, 30 is Shivering, 10 is Slowed, above 0 is Collapse risk, 0 is Collapse.
 */
public enum Hypothermia {
    WARM, COOL, SHIVERING, SLOWED, COLLAPSE_RISK, COLLAPSE;

    public static Hypothermia of(double warmth) {
        if (warmth >= 70) return WARM;
        if (warmth >= 50) return COOL;
        if (warmth >= 30) return SHIVERING;
        if (warmth >= 10) return SLOWED;
        if (warmth > 0) return COLLAPSE_RISK;
        return COLLAPSE;
    }

    /** Shivering burn: +150 kcal/h at 50, rising in a line to +300 at 30, and +300 below that. */
    public static double shiverKcalPerHour(double warmth) {
        if (warmth >= 50) return 0;
        if (warmth <= 30) return 300;
        return 150 + (50 - warmth) / 20.0 * 150;
    }

    /** Walk speed factor: 70% from Slowed down. */
    public double walkFactor() { return ordinal() >= SLOWED.ordinal() ? 0.7 : 1.0; }

    public boolean canRun() { return ordinal() < SLOWED.ordinal(); }

    /** Work time factor: 50% slower from Slowed down. */
    public double workTimeFactor() { return ordinal() >= SLOWED.ordinal() ? 1.5 : 1.0; }

    /** Fine-task timing window: 25% smaller from Shivering down. */
    public double fineWindowFactor() { return ordinal() >= SHIVERING.ordinal() ? 0.75 : 1.0; }

    /** Aim sway, the shaking hands below 30. */
    public boolean aimSway() { return ordinal() >= SLOWED.ordinal(); }

    /** Real seconds of movement between stumbles, or 0 for none. */
    public int stumbleEverySeconds() { return this == COLLAPSE_RISK ? 30 : 0; }
}
