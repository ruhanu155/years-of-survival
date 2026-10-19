package com.yearofice.sim.decay;

/**
 * The {@code state} field of {@code yearofice:fresh} (Minecraft development › Layer 5): raw, cooked, smoked, dried.
 * Raw and cooked food run on the living-rot curve; smoked and dried food count down a fixed life on their own
 * curve (Blueprint › Preserving: "Smoked, dried and rendered food ignores the formula ... and counts down its
 * own life instead"). Frozen is not a state: it is {@code yearofice:item_temp}.
 */
public enum FoodState {
    RAW("raw", false),
    COOKED("cooked", false),
    SMOKED("smoked", true),
    DRIED("dried", true);

    private final String key;
    private final boolean fixedLife;

    FoodState(String key, boolean fixedLife) {
        this.key = key;
        this.fixedLife = fixedLife;
    }

    /** The key used in {@code spoil_days_at_5c} and the component. */
    public String key() { return key; }

    /** True for states that count down a fixed shelf life instead of the living-rot curve. */
    public boolean fixedLife() { return fixedLife; }
}
