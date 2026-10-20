package com.yearofice.sim.body;

import com.yearofice.sim.body.part.PartModel;

/**
 * Condition back per game hour (Blueprint › Health, medicine and nutrition, Condition in numbers): +1.0 asleep in a
 * bed, fed and warm; +0.3 awake and resting; +0.1 working. Underweight and scurvy each halve it; a Rest day doubles
 * it after the halving. Healing stops while a wound bleeds, an infection is active or warmth is under 30 (C1).
 */
public final class Healing {
    private Healing() {}

    public static double baseRate(Activity a, boolean asleep, boolean inBed, double balance24h, double warmth) {
        if (asleep && inBed && balance24h > -1000 && warmth >= 70) return 1.0;
        if (a == Activity.SLEEP || a == Activity.REST) return 0.3;
        return 0.1;
    }

    public static double rate(BodyState s, BodyModel.Inputs in, IllnessModel.Effects ef, boolean restDay,
                              WellnessTuning w) {
        double r = baseRate(in.activity(), in.asleep(), in.asleep() && in.bedC() > 0, s.balance24h(), s.warmth);
        if (s.bmi() < 18.5) r *= 0.5;
        r *= ef.heal();
        if (restDay) r *= w.restHeal();
        return r;
    }

    /** One game hour of healing, if the rules allow it. */
    public static void hour(BodyState s, double rate) {
        if (rate <= 0 || s.parts.condition() >= 100 && s.parts.dSys() <= 0) return;
        if (!PartModel.healingAllowed(s.parts.problems(), s.warmth)) return;
        s.parts = PartModel.healHour(s.parts, rate);
    }
}
