package com.yearofice.sim.body;

import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Digits;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.HeatSource;
import java.util.ArrayList;
import java.util.List;

/**
 * The live frost and exposure half of the body tick (M4-INT): steps every part's frost points through
 * {@link FrostModel#stepMinute} against its zone's exposure temperature and its gear's safe-to limit (the head
 * with the face bonus), keeps the frostbite problem in step with the stage, and solves the "outside like this:
 * about N min" number with {@link ExposureSolver}. Pure: lines go out as {@link BodyState#events}.
 */
public final class FrostStep {
    private FrostStep() {}

    public static final String EV_NIP = "frost_nip", EV_SUPERFICIAL = "frost_superficial", EV_DEEP = "frost_deep",
            EV_DIGIT = "frost_digit_lost";

    /** Steps {@code n} minutes at constant conditions starting at minute {@code m}, then refreshes {@code exposureMin}. */
    public static void tick(BodyState s, EnvSample env, List<HeatSource> heat, WarmthContext ctx, ClothingState clothing,
                            double coldScale, WellnessTuning w, long m, int n) {
        double face = Skin.faceBonusC(s, m, w);
        int[] frost = s.parts.partFrost();
        int[] digits = s.parts.lostDigits();
        List<Problem> problems = new ArrayList<>(s.parts.problems());
        boolean changed = false;
        double[] zoneFeel = new double[Zone.values().length];
        double[] zoneLimit = new double[Zone.values().length];
        for (Zone z : Zone.values()) zoneFeel[z.ordinal()] = Warmth.zoneExposureC(env, clothing, heat, ctx, z);
        for (Part p : Part.values()) {
            int i = p.ordinal();
            double limit = FrostModel.partLimitC(clothing, p, false, face);
            double feel = zoneFeel[p.zone.ordinal()];
            // The zone's limit for the solver: the weakest (warmest) of its parts.
            zoneLimit[p.zone.ordinal()] = zoneLimit[p.zone.ordinal()] == 0 ? limit : Math.max(zoneLimit[p.zone.ordinal()], limit);
            boolean rewarming = feel >= limit;
            if (rewarming && frost[i] == 0) continue;
            int pts = frost[i];
            for (int k = 0; k < n; k++) {
                FrostModel.PartStep st = FrostModel.stepMinute(pts, limit, feel, rewarming, true);
                pts = st.points();
                if (st.stageRose()) {
                    IllnessModel.event(s, st.stageAfter() == 1 ? EV_NIP : st.stageAfter() == 2 ? EV_SUPERFICIAL : EV_DEEP);
                }
                if (st.digitLost()) {
                    digits = Digits.loseNext(digits, p);
                    IllnessModel.event(s, EV_DIGIT);
                }
            }
            if (pts != frost[i]) {
                frost[i] = pts;
                changed = true;
                problems.removeIf(q -> Problems.FROSTBITE.equals(q.id()) && q.part() == p);
                int stage = FrostModel.stage(pts);
                if (stage > 0) problems.add(Problem.fresh(Problems.FROSTBITE, p, "", stage, m));
            }
        }
        if (changed) {
            BodyRecord r = s.parts.withFrost(frost).withDigits(digits);
            s.parts = r.withProblems(problems.size() > 16 ? problems.subList(0, 16) : problems);
        }
        boolean bareHead = Clothing.zoneSafeToC(clothing, Zone.HEAD) >= Clothing.BARE_SAFE_TO_C;
        double[] feels = new double[zoneFeel.length];
        System.arraycopy(zoneFeel, 0, feels, 0, feels.length);
        // The zone limits as the solver reads them: a zone without parts keeps +infinity gap (never binds).
        for (int z = 0; z < zoneLimit.length; z++) if (zoneLimit[z] == 0) zoneLimit[z] = Clothing.BARE_SAFE_TO_C;
        double total = s.lastFeel == null ? env.airC() : s.lastFeel.totalC();
        double t = ExposureSolver.minutes(s.warmth, total, coldScale, bareHead, zoneLimit, feels);
        s.exposureMin = Double.isInfinite(t) ? -1 : ExposureSolver.shown(t);
    }
}
