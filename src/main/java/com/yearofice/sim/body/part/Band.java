package com.yearofice.sim.body.part;

/** Part health bands: Fine 80–100, Sore 60–79, Hurt 30–59, Badly hurt 1–29, Useless 0. */
public enum Band {
    FINE, SORE, HURT, BADLY_HURT, USELESS;

    public static Band of(int hp) {
        if (hp >= 80) return FINE;
        if (hp >= 60) return SORE;
        if (hp >= 30) return HURT;
        if (hp >= 1) return BADLY_HURT;
        return USELESS;
    }

    /** One band lower, for problems that make a part "act one band lower". Head and torso stop at Badly hurt. */
    public Band lower(Part p) {
        Band b = this == USELESS ? USELESS : values()[ordinal() + 1];
        return (p.floor > 0 && b == USELESS) ? BADLY_HURT : b;
    }

    public Band worse(Band o) { return o.ordinal() > ordinal() ? o : this; }

    /** Leg-or-foot walking factor by band. */
    public double walk() { return new double[] {1.0, 0.95, 0.8, 0.6, 0.3}[ordinal()]; }

    /** Leg-or-foot stumble chance factor. */
    public double stumble() { return new double[] {1, 1.5, 2, 3, 3}[ordinal()]; }

    /** Torso useful-time-in-water factor (C11, RECONCILE 123): 10 → 8 → 6 game minutes. */
    public double torsoSwim() { return switch (this) { case HURT -> 0.8; case BADLY_HURT, USELESS -> 0.6; default -> 1; }; }

    /** Arm swing-time factor; {@code NaN} where the band blocks two-handed tools. */
    public double armSwing() { return new double[] {1, 1.1, 1.3, Double.NaN, Double.NaN}[ordinal()]; }

    /** Hand fine-work window factor; {@code NaN} where the band blocks fine work with this hand. */
    public double handWindow() { return new double[] {1, 0.9, 0.75, Double.NaN, Double.NaN}[ordinal()]; }
}
