package com.yearofice.api.body;

/**
 * A read-only view of one player's vitals, as the HUD, the messenger and the journal show them. Everything here
 * is either a stored field of {@code yearofice:body} or derived from it in the same step.
 *
 * @param kcalInToday   absorbed since 00:00
 * @param kcalOutToday  burned since 00:00
 * @param balance24h    absorbed minus burned over the last 24 game hours
 * @param energyCap     the lowest energy cap that applies now
 * @param feelsLikeC    the last step's feels-like, °C
 * @param wetness       overall wetness 0–1, by zone share
 * @param stomachPct    stomach fill 0–1
 * @param macroCarbs    72 h carbs meter 0–1 (likewise fat and protein)
 * @param drunkMlToday  water drunk since 00:00, mL
 */
public record BodyReadout(double kg, double bmi, double kcalInToday, double kcalOutToday, double balance24h,
                          double energy, double energyCap, double warmth, double feelsLikeC, double wetness,
                          double water, double stomachPct, double condition, double strength, double macroCarbs,
                          double macroFat, double macroProtein, double drunkMlToday, boolean gutIll, int sos) {}
