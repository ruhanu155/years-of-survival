package com.yearofice.sim.body;

import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.env.EnvSample;
import java.util.ArrayList;
import java.util.List;

/**
 * Face, glare, wind and hair: B13 sunburn, windburn and grease, B14 beard and hair (Body tab › Injuries on the
 * body). The two skin problems go on the head part as ordinary problems ({@code sunburn}, {@code windburn}); their
 * data lives in {@code injury/}, and the injury engine of the Body tab prices them. This class only decides when
 * one lands and when it ends (2 game days).
 *
 * <p>The adapter fills {@link BodyState#ctxSunDeg}, {@link BodyState#ctxDayOfYear} and {@link BodyState#ctxFace}
 * before a step; with the sun or the date unknown the sun rule is off.
 */
public final class Skin {
    private Skin() {}

    public static final String SUNBURN = "sunburn";
    public static final String WINDBURN = "windburn";
    /** March 1 to April 30 (day of year 60–120). */
    public static final int SUN_FROM_DAY = 60, SUN_TO_DAY = 120;
    public static final double SUN_ABOVE_DEG = 15;
    public static final double WIND_ABOVE_MS = 8, WIND_BELOW_C = -10;
    public static final long BURN_MINUTES = 2 * 1440;
    public static final String[] STAGES = {"Stubble", "Short", "Full", "Long", "Wild"};

    public static boolean greased(BodyState s, long m) { return s.faceGreaseUntil > m; }

    /** Rubs grease on the face for the preset's hours; the new rub replaces the old. */
    public static void greaseFace(BodyState s, long m, WellnessTuning w) {
        s.faceGreaseUntil = m + Math.round(w.faceGreaseHours() * 60);
        s.events.add("face_greased");
    }

    public static void greaseGloves(BodyState s, long m, WellnessTuning w) {
        s.gloveGreaseUntil = m + Math.round(w.gloveGreaseDays() * 1440);
    }

    /** Wetness multiplier on gloves while greased. */
    public static double gloveWetMult(BodyState s, long m, WellnessTuning w) {
        return s.gloveGreaseUntil > m ? w.gloveWetMult() : 1.0;
    }

    /** −1 with the setting off, else 0 Stubble … 4 Wild. */
    public static int beardStage(BodyState s) { return stage(s.hairDays, s.facialHair, STANDARD_DAYS); }

    static final double STANDARD_DAYS = 30;

    public static int stage(double hairDays, boolean on, double stageDays) {
        if (!on) return -1;
        return (int) Math.max(0, Math.min(4, Math.floor(hairDays / stageDays + 1e-6)));
    }

    public static String stageName(int stage) { return stage < 0 ? "none" : STAGES[stage]; }

    /** Degrees the face's safe-to limit drops: grease and a dry Full-or-longer beard (the beard loses it iced). */
    public static double faceBonusC(BodyState s, long m, WellnessTuning w) {
        double c = greased(s, m) ? w.greaseFaceBonusC() : 0;
        if (beardStage(s, w) >= 2 && !s.beardIced) c += w.beardFaceBonusC();
        return c;
    }

    public static int beardStage(BodyState s, WellnessTuning w) { return stage(s.hairDays, s.facialHair, w.beardStageDays()); }

    /** A trim sets the hair back to the start of stage {@code stage}. */
    public static void trim(BodyState s, int stage, WellnessTuning w) {
        s.hairDays = Math.max(0, Math.min(4, stage)) * w.beardStageDays();
        s.beardIced = false;
        s.iceMin = 0;
    }

    /** True when the sun rule can burn a face: spring, on snow, under half cloud, sun above 15°. */
    public static boolean glare(BodyState s, EnvSample e) {
        int d = s.ctxDayOfYear;
        return d >= SUN_FROM_DAY && d <= SUN_TO_DAY && !Double.isNaN(s.ctxSunDeg) && s.ctxSunDeg > SUN_ABOVE_DEG
                && e.snowCm() > 0 && e.cloudPct() < 50;
    }

    public static boolean hardWind(EnvSample e) { return e.windMs() > WIND_ABOVE_MS && e.airC() < WIND_BELOW_C; }

    /** One chunk of {@code n} minutes from {@code m}. {@code outdoors}: not in a room or shelter. */
    public static void tick(BodyState s, WellnessTuning w, EnvSample e, boolean outdoors, boolean nearFire, long m,
                            int n) {
        long end = m + n;
        // Hair grows for any body; the setting only hides it.
        s.hairDays += n / 1440.0;

        // Burns end after two game days.
        List<Problem> keep = null;
        for (Problem p : s.parts.problems()) {
            if ((SUNBURN.equals(p.id()) || WINDBURN.equals(p.id())) && p.nextMark() >= 0 && p.nextMark() <= end) {
                if (keep == null) keep = new ArrayList<>(s.parts.problems());
                keep.remove(p);
            }
        }
        if (keep != null) s.parts = s.parts.withProblems(keep);

        boolean protectedFace = greased(s, m) || s.ctxFace >= 2;
        // Sunburn: a bare face accrues; a ruffed hood halves it.
        if (outdoors && !protectedFace && glare(s, e)) {
            s.sunMin += n * (s.ctxFace == 1 ? 0.5 : 1.0);
        } else {
            s.sunMin = Math.max(0, s.sunMin - n);
        }
        if (s.sunMin >= w.sunburnHours() * 60) {
            s.sunMin = 0;
            burn(s, SUNBURN, end, "face_sunburn");
        }
        // Windburn: two game hours of hard wind on a bare face.
        if (outdoors && !protectedFace && hardWind(e)) {
            s.windMin += n;
        } else {
            s.windMin = Math.max(0, s.windMin - n);
        }
        if (s.windMin >= w.windburnHours() * 60) {
            s.windMin = 0;
            burn(s, WINDBURN, end, "face_windburn");
        }

        // Beard ice: breath below the ice temperature, an hour outdoors, on a Full-or-longer beard.
        boolean beard = beardStage(s, w) >= 2;
        if (beard && outdoors && !nearFire && e.airC() < w.beardIceC()) {
            s.iceMin = Math.min(60, s.iceMin + n);
            if (s.iceMin >= 60 && !s.beardIced) {
                s.beardIced = true;
                s.events.add("beard_iced");
            }
        } else if (!s.beardIced) {
            s.iceMin = Math.max(0, s.iceMin - n);
        }
        if (s.beardIced) {
            if (nearFire) {
                s.iceMin -= n;
                if (s.iceMin <= 50) { // 10 minutes at a fire
                    s.beardIced = false;
                    s.iceMin = 0;
                    s.events.add("beard_thawed");
                }
            }
        }
    }

    private static void burn(BodyState s, String id, long end, String event) {
        for (Problem p : s.parts.problems()) if (id.equals(p.id())) return;
        List<Problem> l = new ArrayList<>(s.parts.problems());
        if (l.size() >= com.yearofice.sim.body.part.BodyRecord.MAX_PROBLEMS) return;
        l.add(Problem.fresh(id, Part.HEAD, "face", 1, end).withTimer(0, end, end + BURN_MINUTES));
        s.parts = s.parts.withProblems(l);
        s.events.add(event);
    }
}
