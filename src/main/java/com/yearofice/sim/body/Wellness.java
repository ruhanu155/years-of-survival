package com.yearofice.sim.body;

import com.yearofice.sim.body.part.PartModel;
import com.yearofice.sim.env.ShelterState;
import java.util.function.Function;

/**
 * Step 8–9 and 11 of the body tick for lane B (Minecraft development › The body tick): illness, vitamin C, Rest days,
 * skin and hair, Condition healing. {@code BodyModel} calls {@link #tick} once per chunk, after the gut clock and
 * before burn, water and energy read its effects.
 */
public final class Wellness {
    private Wellness() {}

    /** Where the dials come from: the preset by default, the toml once the game has loaded it. */
    public static volatile Function<BodyTuning, WellnessTuning> tuningSource = t -> WellnessTuning.of(t.preset());

    public static WellnessTuning tuning(BodyTuning t) { return tuningSource.apply(t); }

    /** The folded effects and the extra burn a Rest day's healing costs (10% of BMR a day, while healing). */
    public record Result(IllnessModel.Effects effects, double extraKcal) {}

    /** The warmth term fever's chills add to the feels-like (°C, ≤ 0), for the cycle at minute {@code m}. */
    public static double chillC(BodyState s, long m) { return IllnessModel.effects(s, m).chillC(); }

    public static Result tick(BodyState s, BodyModel.Inputs in, BodyTuning t, long m, int n, boolean nearHeat) {
        WellnessTuning w = tuning(t);
        IllnessModel.Effects ef = IllnessModel.effects(s, m);
        IllnessModel.tick(s, t, w, m, n, in.asleep());

        boolean nearWarm = nearHeat || in.shelter().bonusC() >= ShelterState.OUTPOST_CABIN.bonusC();
        RestDay.tick(s, in.activity(), in.asleep(), nearWarm, m, n);
        boolean outdoors = in.env().roomId() == 0 && in.shelter().bonusC() < ShelterState.OPEN_ENCLOSURE.bonusC();
        Skin.tick(s, w, in.env(), outdoors, nearHeat, m, n);

        // Ongoing systemic losses (botulism, taste-test sickness...) and the Condition cap (trichinella).
        if (ef.condPerDay() > 0) s.parts = PartModel.systemic(s.parts, ef.condPerDay() * n / 1440.0);

        // Healing as each game hour closes.
        double extra = 0;
        boolean rest = RestDay.unbroken(s) && nearWarm;
        if (Math.floorMod(m + n, 60) == 0) {
            Healing.hour(s, Healing.rate(s, in, ef, rest, w));
        }
        if (s.parts.condition() > ef.condCap()) s.parts = PartModel.systemic(s.parts, s.parts.condition() - ef.condCap());
        if (rest && s.parts.condition() < 100) extra = 0.10 * s.bmr() / 1440.0 * n;
        return new Result(ef, extra);
    }
}
