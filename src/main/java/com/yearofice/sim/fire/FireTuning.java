package com.yearofice.sim.fire;

/**
 * Every fire tuning number, as the {@code [fire]} section of {@code yearofice-common.toml} holds them. The sim
 * reads only this record; {@code com.yearofice.fire.FireConfig} fills it from the config, and tests use
 * {@link #standard()}. Sources: Blueprint › Fire, water and light (states, settings, rain and snow, Building a
 * campfire) unless a field says otherwise.
 *
 * @param ringArmloadMin     a plain ring's burn minutes per armload of a ×1.0 wood at Steady. The Blueprint gives
 *                           burn times only per build (teepee, long fire, Dakota hole, stove: M3); 40 is the slice's
 *                           plain ring, chosen so two dead-spruce armloads (×0.7) burn about 56 minutes and the
 *                           M2.5 test's "coals in 60 ±10" holds.
 * @param armloadCatchMin    minutes for an armload to catch from a small fire or coals.
 * @param emberLifeS         real seconds an ember or a match flame lives before it dies (60 s; halved in wind).
 * @param emberWindMs        wind over this halves the ember's life unless the player is sheltered (6 m/s).
 * @param tinderFlameMin     game minutes a tinder flame lasts (2).
 * @param smallFireMin       game minutes a small fire lasts on its kindling (20).
 * @param coalsMin           game minutes coals last unfed (60).
 * @param bankedMin          game minutes banked coals keep (360: "6 hours").
 * @param kindlingNeeded     kindling pieces a lay needs before the tinder can catch it ("two handfuls").
 * @param heavyRainStepMin   heavy rain drops an uncovered fire one state every this many minutes (10).
 * @param lightRainStepMin   light rain does the same every this many minutes (30).
 * @param snowLowOutMin      snowfall puts out a Low fire in this many minutes (20).
 * @param snowBurnFactor     snowfall slows the other fires by a quarter: fuel use × 0.75.
 * @param windBurnMs         slice rule: over this wind an uncovered fire eats fuel faster.
 * @param windBurnFactor     the fuel-use factor above {@code windBurnMs}.
 * @param tinderMaxMoisture  tinder and kindling catch only at this moisture or less (dryness 60+, Items › Wet).
 * @param matchMaxMoisture   a match above this moisture fails every time (20).
 * @param matchCalmPct       match success in calm air (95).
 * @param matchWindPct       match success in wind (50).
 * @param matchWindMs        "in wind" for matches means this wind or more.
 * @param drillFreshPct      bow-drill success for a fresh player in Automatic mode (70).
 * @param drillPracticedPct  bow-drill success at Practiced (95).
 * @param drillSkillReps     N for the Fire skill: reps to Practiced (10, Crafting › Practice).
 * @param drillKcal          kcal a bow-drill try costs, whether it works or not (60).
 * @param drillMinEnergy     energy bolts below which a bow-drill attempt ends (2).
 * @param drillWindPct       Automatic-mode success × this in wind over {@code emberWindMs} without shelter (85).
 * @param packedArmloads     more armloads than this packs an open fire too full (3).
 * @param packedHotArmloads  more armloads than this packs a Hot fire too full (2).
 * @param maxArmloads        most armloads a ring holds.
 * @param rainWetsPerHour    moisture points a laid, unlit part gains an hour in uncovered rain (20, Items › Wet).
 */
public record FireTuning(
        double ringArmloadMin,
        int armloadCatchMin,
        int emberLifeS,
        double emberWindMs,
        int tinderFlameMin,
        int smallFireMin,
        int coalsMin,
        int bankedMin,
        int kindlingNeeded,
        int heavyRainStepMin,
        int lightRainStepMin,
        int snowLowOutMin,
        double snowBurnFactor,
        double windBurnMs,
        double windBurnFactor,
        int tinderMaxMoisture,
        int matchMaxMoisture,
        int matchCalmPct,
        int matchWindPct,
        double matchWindMs,
        int drillFreshPct,
        int drillPracticedPct,
        int drillSkillReps,
        int drillKcal,
        double drillMinEnergy,
        int drillWindPct,
        int packedArmloads,
        int packedHotArmloads,
        int maxArmloads,
        double rainWetsPerHour) {

    public static FireTuning standard() { return FirePreset.STANDARD.tuning; }
}
