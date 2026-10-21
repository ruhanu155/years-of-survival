package com.yearofice.sim.weather;

/** Lightning harm to a player: the dial {@code lightning_player_risk} is the chance at the strike point, fading to 0 at 15 m. */
public final class StrikeRisk {
    private StrikeRisk() {}

    public static final double REACH_M = 15;

    public static double chance(double distM, double risk, boolean openSky) {
        if (!openSky || distM >= REACH_M) return 0;
        return Math.min(1, risk * (1 - distM / REACH_M));
    }
}
