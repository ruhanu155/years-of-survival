package com.yearofice.sim.body;

/**
 * The preset defaults the three borrowed cores read (Blueprint › Realism presets; Body tab › Tuning).
 * At runtime the values come from {@code yearofice-common.toml}; this mirror keeps the sim free of
 * Minecraft classes and gives tests the Standard numbers. Sliders pass their own values to the
 * functions directly.
 */
public enum BodyPreset {
    GENTLE(0.5, 0.15, 0.5, 0.5, false),
    STANDARD(1.0, 0.30, 1.0, 1.0, true),
    BRUTAL(1.3, 0.40, 1.5, 1.5, true);

    /** Cold: warmth loss rate ×. */
    public final double coldScale;
    /** Unboiled water illness chance. */
    public final double unboiledIllness;
    public final double bleedScale;
    public final double infectionScale;
    /** {@code body.digit_loss}: deep frostbite can take digits. */
    public final boolean digitLoss;

    BodyPreset(double cold, double unboiled, double bleed, double infection, boolean digitLoss) {
        this.coldScale = cold;
        this.unboiledIllness = unboiled;
        this.bleedScale = bleed;
        this.infectionScale = infection;
        this.digitLoss = digitLoss;
    }
}
