package com.yearofice.sim.body;

import java.util.Locale;

/**
 * The M4 lane B dials of {@code yearofice-common.toml [body.*]} (Body tab › Tuning and debug; B13, B14, Rest days):
 * how long skin takes to burn, how long grease and beards last, how much a Rest day helps. Kept apart from
 * {@link BodyTuning} so the two lanes' dials never share a constructor. Pure Java.
 *
 * @param illnessScale   × on the duration of every timed illness the body steps (Gentle shorter, Brutal longer)
 * @param restHeal       × on Condition healing on a Rest day (2)
 * @param feverSleepMult x on sleep recovery while a fever shows (0.7)
 * @param gloveWetMult   × on a greased glove's wetness gain (0.7)
 */
public record WellnessTuning(double illnessScale, double restHeal, double sunburnHours, double windburnHours,
                             double faceGreaseHours, double gloveGreaseDays, double gloveWetMult,
                             double beardStageDays, double beardFaceBonusC, double beardIceC, double greaseFaceBonusC,
                             double feverSleepMult) {

    public static final WellnessTuning GENTLE = new WellnessTuning(0.75, 2, 3, 2, 8, 3, 0.7, 30, 2, -25, 2, 0.7);
    public static final WellnessTuning STANDARD = new WellnessTuning(1.0, 2, 3, 2, 8, 3, 0.7, 30, 2, -25, 2, 0.7);
    public static final WellnessTuning BRUTAL = new WellnessTuning(1.25, 2, 3, 2, 8, 3, 0.7, 30, 2, -25, 2, 0.7);

    public static WellnessTuning of(String preset) {
        String p = preset == null ? "" : preset.toLowerCase(Locale.ROOT);
        if (p.startsWith("gentle")) return GENTLE;
        if (p.startsWith("brutal")) return BRUTAL;
        return STANDARD;
    }
}
