package com.yearofice.sim.body;

import com.yearofice.sim.body.part.PartModel;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.ShelterState;
import java.util.List;

/**
 * The body tick's model: {@code step(record, inputs, minutes)} (Minecraft development › Layer 7 and The body
 * tick). This is the M2.5 slice of the 13 steps: 1 sample (the adapter's job), 2 burn (times the load band's
 * calorie factor), 3 stomach and absorb,
 * 4 weight, 5 water, 6 warmth, 7 energy, 10 derive, 12 check. Condition is held (no illness or injury
 * stepping yet); steps 8, 9 and 11 are M4.
 *
 * <p><b>Closed form.</b> Up to 10 minutes are stepped one at a time. Above that the model steps in chunks that
 * end on the hour: burn, weight, stomach, water and energy are linear over a chunk at the chunk's starting rates
 * (BMR recomputed each hour), while warmth and wetness still walk minute by minute through their own cores,
 * because the soaked rule and the 0 clamp are not linear. GameTest {@code body/closed_form_matches} holds the two
 * paths within 0.5%.
 */
public final class BodyModel {
    private BodyModel() {}

    public static final int CLOSED_FORM_ABOVE = 10;

    /** What made the body collapse, if anything. */
    public enum Collapse { NONE, BMI, WARMTH, CONDITION }

    /**
     * One step's inputs, held constant across the minutes it covers.
     *
     * @param heat          heat sources around the player; {@code null} reads {@code env.heatBonusC()} instead
     * @param sleepQuality  bed quality times the other sleep factors, 0–1 (used only when asleep)
     * @param moving        moving this minute (drying rate)
     * @param loadKcal      the load band's calorie factor (Layer 6, Load: 1.0 under 15 kg, 1.2, 1.5), on the
     *                      activity burn while awake; shivering and sleep are not carrying
     * @param bedC          the bed's insulation while asleep, °C (bough bed +5; bare ground 0), added on top of the
     *                      2 °C sleeping chill (Blueprint › Sleep, Sleeping cold; Making camp)
     */
    public record Inputs(Activity activity, boolean asleep, double sleepQuality, EnvSample env, List<HeatSource> heat,
                         ShelterState shelter, ColdWater.Depth depth, boolean moving, double loadKcal, double bedC) {
        public Inputs {
            if (activity == null) activity = Activity.REST;
            if (shelter == null) shelter = ShelterState.NONE;
            if (depth == null) depth = ColdWater.Depth.NONE;
            if (asleep) activity = Activity.SLEEP;
            if (!(loadKcal > 0)) loadKcal = 1.0;
            if (!Double.isFinite(bedC)) bedC = 0;
        }

        /** Unloaded inputs with no bed insulation (load factor 1; awake, or asleep on bare ground). */
        public Inputs(Activity activity, boolean asleep, double sleepQuality, EnvSample env, List<HeatSource> heat,
                      ShelterState shelter, ColdWater.Depth depth, boolean moving) {
            this(activity, asleep, sleepQuality, env, heat, shelter, depth, moving, 1.0, 0);
        }

        public static Inputs resting(EnvSample env) {
            return new Inputs(Activity.REST, false, 0, env, List.of(), ShelterState.NONE, ColdWater.Depth.NONE, false);
        }

        public Inputs doing(Activity a) {
            boolean sleep = a == Activity.SLEEP;
            return new Inputs(a, sleep, sleep && sleepQuality <= 0 ? 1.0 : sleepQuality, env, heat, shelter, depth,
                    a == Activity.WALK || a == Activity.RUN || a == Activity.HEAVY, loadKcal, bedC);
        }

        public Inputs in(EnvSample e) {
            return new Inputs(activity, asleep, sleepQuality, e, heat, shelter, depth, moving, loadKcal, bedC);
        }

        public Inputs inWater(ColdWater.Depth d) {
            return new Inputs(activity, asleep, sleepQuality, env, heat, shelter, d, moving, loadKcal, bedC);
        }

        public Inputs withHeat(List<HeatSource> h) {
            return new Inputs(activity, asleep, sleepQuality, env, h, shelter, depth, moving, loadKcal, bedC);
        }

        /** The same inputs on a bed of {@code q} quality and {@code c} °C insulation. */
        public Inputs onBed(double q, double c) {
            return new Inputs(activity, asleep, q, env, heat, shelter, depth, moving, loadKcal, c);
        }

        public Inputs carrying(double kcalFactor) {
            return new Inputs(activity, asleep, sleepQuality, env, heat, shelter, depth, moving, kcalFactor, bedC);
        }

        /** The load factor on this step's activity burn: 1 while asleep. */
        public double burnFactor() { return asleep ? 1.0 : loadKcal; }
    }

    /** The outcome of a step: the first collapse and the game minute it happened, or NONE. */
    public record Result(Collapse collapse, long minute) {
        public static final Result NONE = new Result(Collapse.NONE, -1);

        public boolean collapsed() { return collapse != Collapse.NONE; }
    }

    /**
     * Steps {@code minutes} game minutes starting at absolute game minute {@code start}. Stops at the first
     * collapse; the caller hands it to the SOS flow.
     */
    public static Result step(BodyState s, Inputs in, BodyTuning t, long start, int minutes) {
        long m = start;
        long end = start + minutes;
        boolean closed = minutes > CLOSED_FORM_ABOVE;
        while (m < end) {
            int n = closed ? (int) Math.min(end - m, 60 - Math.floorMod(m, 60)) : 1;
            Result r = chunk(s, in, t, m, n);
            if (r.collapsed()) return r;
            m += n;
        }
        return Result.NONE;
    }

    private static Result chunk(BodyState s, Inputs in, BodyTuning t, long m, int n) {
        s.advanceBins(m);
        int hourOfDay = (int) Math.floorMod(Math.floorDiv(m, 60), 24);
        Activity a = in.activity();
        s.lastActivity = a;
        s.lastAirC = in.env().airC();

        // 6. Warmth and wetness, minute by minute (the soaked rule and the clamps are not linear).
        ClothingState clothing = s.clothing();
        WetnessModel.Exposure exposure = WetnessModel.Exposure.of(in.env(), in.shelter().rainFactor(), a, in.moving(),
                        heatNear(in))
                .in(in.depth());
        WarmthContext ctx = new WarmthContext(in.shelter(), a,
                Metabolism.bmiWarmthC(s.bmi()) + (in.asleep() ? Metabolism.SLEEP_FEEL_C + in.bedC() : 0)
                        + Wellness.chillC(s, m));
        double gloveMult = Skin.gloveWetMult(s, m, Wellness.tuning(t));
        double shiver = 0;
        Collapse collapse = Collapse.NONE;
        int done = 0;
        for (int k = 0; k < n; k++) {
            clothing = WetnessModel.stepMinute(clothing, exposure, gloveMult);
            FeelsLike feel = Warmth.feelsLike(in.env(), clothing, in.heat(), ctx);
            Warmth.Step ws = Warmth.Step.of(feel, clothing, in.env(), in.heat() == null ? List.of() : in.heat(),
                    t.coldScale());
            if (in.depth() != ColdWater.Depth.NONE) ws = ws.inWater(in.env().waterC(), in.depth());
            s.warmth = Warmth.step(s.warmth, ws);
            s.lastFeel = feel;
            shiver += Hypothermia.shiverKcalPerHour(s.warmth) / 60.0;
            done = k + 1;
            if (s.warmth <= 0) {
                collapse = Collapse.WARMTH;
                break;
            }
        }
        double[] wet = clothing.wetCopy();
        System.arraycopy(wet, 0, s.slotWet, 0, wet.length);

        // 8–9, 11 (lane B). Illness, vitamin C, Rest day, skin and hair, healing.
        Wellness.Result wr = Wellness.tick(s, in, t, m, done, heatNear(in));
        IllnessModel.Effects ill = wr.effects();

        // 2–4. Burn, absorb, weight.
        double burned = Metabolism.kcalPerHour(a) / 60.0 * s.bmr() / Metabolism.DEFAULT_BMR * t.calorieBurn() * done
                * in.burnFactor() * ill.burn() + shiver + wr.extraKcal();
        double absorbed = s.pendingKcal;
        s.pendingKcal = 0;
        s.kg += (absorbed - burned) / t.kcalPerKg();
        s.kcalOut[hourOfDay] += burned;
        s.stomachKg = Math.max(0, s.stomachKg - Metabolism.stomachEmptyPerMinute(in.asleep(), t.stomachKg()) * done);

        // 5. Water.
        boolean gut = s.gutIll(m);
        s.water = clamp(s.water - (Metabolism.waterDrainPerMinute(a, in.env().airC(), gut)
                + ill.waterLDay() * 1000 / Metabolism.ML_PER_POINT / 1440) * t.waterNeed() * done, 0, 100);

        // 10 (part). The cap, before energy reads it.
        if (s.gutOnset >= 0 && m + done >= s.gutOnset) {
            s.gutUntil = s.gutOnset + Metabolism.GUT_ILL_MINUTES;
            // Fever on day 1 of a gut illness: a 3-hour cycle, chills in the last hour.
            IllnessModel.start(s, "fever", s.gutOnset, s.gutOnset + 1440);
            s.gutOnset = -1;
            IllnessModel.event(s, "ill_gut_illness_1");
        }
        if (s.gutUntil >= 0 && m + done >= s.gutUntil) {
            s.gutUntil = -1;
            IllnessModel.event(s, "ill_gut_illness_end");
        }
        double cap = Metabolism.energyCap(s.balance24h(), s.bmi(), s.water, s.awakeMin, s.gutIll(m));
        cap = Math.min(cap, ill.energyCap());
        s.lastEnergyCap = cap;

        // 7. Energy and sleep debt.
        if (in.asleep()) {
            if (s.energy < cap) {
                s.energy = Math.min(cap, s.energy + Metabolism.SLEEP_BOLTS_PER_H * in.sleepQuality() * SleepRules.feverFactor(s, m, Wellness.tuning(t)) / 60.0 * done);
            }
            s.sleepRun += done;
            if (s.sleepRun >= 240) s.awakeMin = 0;
            s.asleep = true;
            s.sleepQ = in.sleepQuality();
        } else {
            s.energy = Math.max(0, s.energy + Metabolism.energyPerHourAwake(a) / 60.0 * done);
            s.awakeMin += done;
            s.sleepRun = 0;
            s.asleep = false;
        }

        // 12. Check.
        if (collapse != Collapse.NONE) return new Result(collapse, m + done - 1);
        if (s.bmi() <= t.sosBmi()) return new Result(Collapse.BMI, m + done - 1);
        if (s.condition() <= 0) return new Result(Collapse.CONDITION, m + done - 1);
        return Result.NONE;
    }

    private static boolean heatNear(Inputs in) {
        if (in.heat() == null) return in.env().heatBonusC() > 0;
        for (HeatSource h : in.heat()) if (h.isFire() && h.distanceM() <= Warmth.FIRE_RESCUE_M) return true;
        return false;
    }

    /**
     * Food reaches the body: mass to the stomach, kcal and macros to today's bins and the next step's absorb.
     * Returns false and changes nothing when the stomach is full.
     */
    public static boolean eat(BodyState s, BodyTuning t, long minute, double grams, double kcal, double carbsG,
                              double fatG, double proteinG, double waterPoints) {
        if (s.stomachKg >= t.stomachKg()) return false;
        s.advanceBins(minute);
        double gut = IllnessModel.absorb(s, minute);
        s.stomachKg = Math.min(t.stomachKg() * 1.05, s.stomachKg + grams / 1000.0);
        double absorbed = kcal * gut;
        s.pendingKcal += absorbed;
        int d = (int) Math.floorMod(Math.floorDiv(minute, 60), 24);
        s.kcalIn[d] += absorbed;
        int mh = (int) Math.floorMod(Math.floorDiv(minute, 60), BodyState.MACRO_HOURS) * 3;
        s.macroG[mh + Metabolism.CARBS] += carbsG;
        s.macroG[mh + Metabolism.FAT] += fatG;
        s.macroG[mh + Metabolism.PROTEIN] += proteinG;
        s.water = clamp(s.water + waterPoints, 0, 100);
        return true;
    }

    /**
     * Work charged by another system (a bow-drill try is 60 kcal): booked in this hour's burn and taken off the next
     * step's absorb, so weight moves by it at the next step like any other burn.
     */
    public static void charge(BodyState s, long minute, double kcal) {
        if (kcal <= 0) return;
        s.advanceBins(minute);
        s.kcalOut[(int) Math.floorMod(Math.floorDiv(minute, 60), 24)] += kcal;
        s.pendingKcal -= kcal;
    }

    /** A drink of {@code litres}. A bad roll starts the gut-illness clock {@code onsetMin} minutes later. */
    public static void drink(BodyState s, long minute, double litres, int onsetMin) {
        s.advanceBins(minute);
        s.water = clamp(s.water + Metabolism.waterPoints(litres), 0, 100);
        s.drunkMl[(int) Math.floorMod(Math.floorDiv(minute, 60), 24)] += litres * 1000;
        if (onsetMin >= 0) {
            s.gutOnset = minute + onsetMin;
        }
    }

    /**
     * The return after a flight out (Blueprint › Collapse and SOS, step 5): BMI 19, Condition 80, warmth, water
     * and energy full, dry, stomach empty. Lost digits and crooked parts stay.
     */
    public static void sosReturn(BodyState s, BodyTuning t) {
        s.kg = Metabolism.kgForBmi(t.returnBmi(), s.heightCm);
        s.parts = PartModel.sosReturn(s.parts);
        s.warmth = 100;
        s.water = 100;
        s.energy = 10;
        s.stomachKg = 0;
        s.pendingKcal = 0;
        s.awakeMin = 0;
        s.sleepRun = 0;
        s.asleep = false;
        s.gutOnset = -1;
        s.gutUntil = -1;
        s.ill.clear();
        s.nausea = 0;
        s.vitC = IllnessModel.VITC_FULL_DAYS;
        s.sos = 0;
        java.util.Arrays.fill(s.slotWet, 0);
    }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
