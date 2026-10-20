package com.yearofice.sim.hunt;

/**
 * Every hunting tuning number, as the {@code [hunt]} section of {@code yearofice-common.toml} holds them
 * ({@code com.yearofice.hunt.HuntConfig} fills it; tests use {@link #standard()}). Sources: Blueprint › Weapons and
 * arrowheads, The hit-zone model, Setting up the hunt, Calling animals.
 *
 * @param gravityMps2       arrow gravity (9.81)
 * @param dragPerS          linear drag toward the air's velocity, so wind pushes the arrow (0.08)
 * @param swayCmAt20m       sway radius at 20 m with no modifiers (4 cm, Draw and hold)
 * @param holdBoltsPerS     draw-hold energy cost (0.02 bolts a second)
 * @param rangeFalloffPerM  f_range slope past the effective range (0.04 a metre)
 * @param cappedDrawPct     full draw cap when Strength is under the bow's requirement (60%)
 * @param lowEnergyDrawX    draw time x this below 3 bolts (1.5)
 * @param bloodLifeMin      game minutes a blood drop lasts (120)
 * @param bloodRainX        drop life x this in rain (0.5)
 * @param bloodSnowX        drop life x this in snow (2.0)
 * @param pushExtraDeathMin game minutes added to a pushed animal's death timer (120)
 * @param pushTrailX        blood drops x this after a push (0.5)
 * @param callRutPeak       cow-moan base odds (0.4)
 * @param blindSightX       sight range factor against a still player in a blind (0.0 means unseen; 0.1 floor)
 * @param blindFreshSightX  sight factor for a blind under one game day old (0.5)
 * @param standPreySightX   tree stand: prey see the player at this factor (0.3)
 * @param standBearSightX   tree stand: bears at this factor (0.7)
 * @param standScentClearM  scent does not reach the ground within this many metres (40)
 * @param bluffShare        share of bear charges that are bluffs for an unwounded black bear (0.7)
 */
public record HuntTuning(double gravityMps2, double dragPerS, double swayCmAt20m, double holdBoltsPerS, double rangeFalloffPerM,
                         double cappedDrawPct, double lowEnergyDrawX, double bloodLifeMin, double bloodRainX, double bloodSnowX,
                         double pushExtraDeathMin, double pushTrailX, double callRutPeak, double blindSightX, double blindFreshSightX,
                         double standPreySightX, double standBearSightX, double standScentClearM, double bluffShare) {

    public static HuntTuning standard() { return HuntPreset.STANDARD.tuning; }
}
