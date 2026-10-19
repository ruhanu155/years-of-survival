package com.yearofice.sim.env;

/** Open-fire settings and their warmth within 3 m (Blueprint › Warmth › Fire). */
public enum FireSetting {
    LOW(10),
    STEADY(18),
    HOT(25);

    public final double bonusC;

    FireSetting(double bonusC) { this.bonusC = bonusC; }
}
