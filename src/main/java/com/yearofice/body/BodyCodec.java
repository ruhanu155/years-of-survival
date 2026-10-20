package com.yearofice.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The versioned codec of {@code yearofice:body} (Minecraft development › The body attachment; Body tab › The body
 * record, extended). Keys are the snake_case field names of the record table. Every field has a default, so a
 * schema 1 save (no {@code part_hp}) loads, and the upgrade then sets every part to 100 and moves the old
 * Condition loss into {@code d_sys}. The codec always writes the current schema (3: illness, vitamin C, Rest days, skin and hair).
 *
 * <p>The record is flat in the save, as the Blueprint's JSON shows it; three map codecs (vitals, bins, parts)
 * share one object because a record codec takes at most 16 fields.
 */
public final class BodyCodec {
    private BodyCodec() {}

    private static Codec<List<Integer>> ints() { return Codec.INT.listOf(); }

    private static List<Integer> toInts(double[] a) {
        List<Integer> l = new ArrayList<>(a.length);
        for (double v : a) l.add((int) Math.round(v));
        return l;
    }

    private static List<Integer> toInts(int[] a) { return Arrays.stream(a).boxed().toList(); }

    private static void fill(double[] dst, List<Integer> src) {
        Arrays.fill(dst, 0);
        for (int i = 0; i < Math.min(dst.length, src.size()); i++) dst[i] = src.get(i);
    }

    private static int[] intArray(List<Integer> src, int len, int def) {
        int[] a = new int[len];
        Arrays.fill(a, def);
        for (int i = 0; i < Math.min(len, src.size()); i++) a[i] = src.get(i);
        return a;
    }

    // --- vitals --------------------------------------------------------------------------------------------
    record Vitals(int schema, int heightCm, int sex, float kg, float stomachKg, float water, float warmth, float energy,
                  int awakeMin, boolean asleep, int sleepRun, float sleepQ, int sos, float pendingKcal) {}

    static final MapCodec<Vitals> VITALS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("schema", 1).forGetter(Vitals::schema),
            Codec.INT.optionalFieldOf("height_cm", 183).forGetter(Vitals::heightCm),
            Codec.INT.optionalFieldOf("sex", 0).forGetter(Vitals::sex),
            Codec.FLOAT.optionalFieldOf("kg", 77f).forGetter(Vitals::kg),
            Codec.FLOAT.optionalFieldOf("stomach_kg", 0f).forGetter(Vitals::stomachKg),
            Codec.FLOAT.optionalFieldOf("water", 100f).forGetter(Vitals::water),
            Codec.FLOAT.optionalFieldOf("warmth", 100f).forGetter(Vitals::warmth),
            Codec.FLOAT.optionalFieldOf("energy", 10f).forGetter(Vitals::energy),
            Codec.INT.optionalFieldOf("awake_min", 0).forGetter(Vitals::awakeMin),
            Codec.BOOL.optionalFieldOf("asleep", false).forGetter(Vitals::asleep),
            Codec.INT.optionalFieldOf("sleep_run", 0).forGetter(Vitals::sleepRun),
            Codec.FLOAT.optionalFieldOf("sleep_q", 0f).forGetter(Vitals::sleepQ),
            Codec.INT.optionalFieldOf("sos", 0).forGetter(Vitals::sos),
            Codec.FLOAT.optionalFieldOf("pending_kcal", 0f).forGetter(Vitals::pendingKcal)
    ).apply(i, Vitals::new));

    // --- bins ----------------------------------------------------------------------------------------------
    record Bins(List<Integer> kcalIn, List<Integer> kcalOut, List<Integer> drunkMl, List<Integer> macroG, long binHour,
                long gutOnset, long gutUntil, List<Float> slotWet) {}

    static final MapCodec<Bins> BINS = RecordCodecBuilder.mapCodec(i -> i.group(
            ints().optionalFieldOf("kcal_in", List.of()).forGetter(Bins::kcalIn),
            ints().optionalFieldOf("kcal_out", List.of()).forGetter(Bins::kcalOut),
            ints().optionalFieldOf("drunk_ml", List.of()).forGetter(Bins::drunkMl),
            ints().optionalFieldOf("macro_g", List.of()).forGetter(Bins::macroG),
            Codec.LONG.optionalFieldOf("bin_hour", -1L).forGetter(Bins::binHour),
            Codec.LONG.optionalFieldOf("gut_onset", -1L).forGetter(Bins::gutOnset),
            Codec.LONG.optionalFieldOf("gut_until", -1L).forGetter(Bins::gutUntil),
            Codec.FLOAT.listOf().optionalFieldOf("slot_wet", List.of()).forGetter(Bins::slotWet)
    ).apply(i, Bins::new));

    // --- parts (the Body tab's schema-2 fields) ------------------------------------------------------------
    static final Codec<Part> PART = Codec.STRING.xmap(Part::byId, p -> p.name().toLowerCase(java.util.Locale.ROOT));

    record ProblemA(String id, Part part, String sub, int stage, int timer, boolean dressed, boolean infected,
                    String pad) {}

    record ProblemB(String binding, long cleanUntil, long splint, boolean sling, long wrap, boolean cooled,
                    long started, long nextMark) {}

    static final MapCodec<ProblemA> PROBLEM_A = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ProblemA::id),
            PART.fieldOf("part").forGetter(ProblemA::part),
            Codec.STRING.optionalFieldOf("sub", "").forGetter(ProblemA::sub),
            Codec.INT.optionalFieldOf("stage", 1).forGetter(ProblemA::stage),
            Codec.INT.optionalFieldOf("timer", 0).forGetter(ProblemA::timer),
            Codec.BOOL.optionalFieldOf("dressed", false).forGetter(ProblemA::dressed),
            Codec.BOOL.optionalFieldOf("infected", false).forGetter(ProblemA::infected),
            Codec.STRING.optionalFieldOf("pad", "").forGetter(ProblemA::pad)
    ).apply(i, ProblemA::new));

    static final MapCodec<ProblemB> PROBLEM_B = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("binding", "").forGetter(ProblemB::binding),
            Codec.LONG.optionalFieldOf("clean_until", -1L).forGetter(ProblemB::cleanUntil),
            Codec.LONG.optionalFieldOf("splint", -1L).forGetter(ProblemB::splint),
            Codec.BOOL.optionalFieldOf("sling", false).forGetter(ProblemB::sling),
            Codec.LONG.optionalFieldOf("wrap", -1L).forGetter(ProblemB::wrap),
            Codec.BOOL.optionalFieldOf("cooled", false).forGetter(ProblemB::cooled),
            Codec.LONG.optionalFieldOf("started", 0L).forGetter(ProblemB::started),
            Codec.LONG.optionalFieldOf("next_mark", -1L).forGetter(ProblemB::nextMark)
    ).apply(i, ProblemB::new));

    public static final Codec<Problem> PROBLEM = RecordCodecBuilder.create(i -> i.group(
            PROBLEM_A.forGetter(p -> new ProblemA(p.id(), p.part(), p.sub(), p.stage(), p.timer(), p.dressed(),
                    p.infected(), p.pad())),
            PROBLEM_B.forGetter(p -> new ProblemB(p.binding(), p.cleanUntil(), p.splint(), p.sling(), p.wrap(),
                    p.cooled(), p.started(), p.nextMark()))
    ).apply(i, (a, b) -> new Problem(a.id(), a.part(), a.sub(), a.stage(), a.timer(), a.dressed(), a.infected(),
            a.pad(), b.binding(), b.cleanUntil(), b.splint(), b.sling(), b.wrap(), b.cooled(), b.started(),
            b.nextMark())));

    record Parts(float condition, float dSys, List<Integer> partHp, List<Integer> partFrost, List<Integer> lostDigits,
                 int crooked, int injurySeq, List<Problem> problems) {}

    static final MapCodec<Parts> PARTS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("condition", 100f).forGetter(Parts::condition),
            Codec.FLOAT.optionalFieldOf("d_sys", -1f).forGetter(Parts::dSys),
            ints().optionalFieldOf("part_hp", List.of()).forGetter(Parts::partHp),
            ints().optionalFieldOf("part_frost", List.of()).forGetter(Parts::partFrost),
            ints().optionalFieldOf("lost_digits", List.of(0, 0)).forGetter(Parts::lostDigits),
            Codec.INT.optionalFieldOf("crooked", 0).forGetter(Parts::crooked),
            Codec.INT.optionalFieldOf("injury_seq", 0).forGetter(Parts::injurySeq),
            PROBLEM.listOf().optionalFieldOf("problems", List.of()).forGetter(Parts::problems)
    ).apply(i, Parts::new));

    // --- illness, rest, skin and hair (schema 3, lane B) ----------------------------------------------------
    record Ill(String id, long start, long until) {}

    static final Codec<Ill> ILL = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Ill::id),
            Codec.LONG.fieldOf("start").forGetter(Ill::start),
            Codec.LONG.optionalFieldOf("until", -1L).forGetter(Ill::until)
    ).apply(i, Ill::new));

    record Wellness(float vitC, float nausea, List<Ill> ill, int restRun, float restWork, int restAway, int restSleep,
                    long restDay, float hairDays, boolean facialHair, long faceGrease, long gloveGrease, float sunMin,
                    float windMin, float iceMin) {}

    static final MapCodec<Wellness> WELLNESS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("vit_c", 30f).forGetter(Wellness::vitC),
            Codec.FLOAT.optionalFieldOf("nausea", 0f).forGetter(Wellness::nausea),
            ILL.listOf().optionalFieldOf("ill", List.of()).forGetter(Wellness::ill),
            Codec.INT.optionalFieldOf("rest_run", 0).forGetter(Wellness::restRun),
            Codec.FLOAT.optionalFieldOf("rest_work_min", 0f).forGetter(Wellness::restWork),
            Codec.INT.optionalFieldOf("rest_away_min", 0).forGetter(Wellness::restAway),
            Codec.INT.optionalFieldOf("rest_sleep_min", 0).forGetter(Wellness::restSleep),
            Codec.LONG.optionalFieldOf("rest_day", -1L).forGetter(Wellness::restDay),
            Codec.FLOAT.optionalFieldOf("hair_days", 0f).forGetter(Wellness::hairDays),
            Codec.BOOL.optionalFieldOf("facial_hair", true).forGetter(Wellness::facialHair),
            Codec.LONG.optionalFieldOf("face_grease_until", -1L).forGetter(Wellness::faceGrease),
            Codec.LONG.optionalFieldOf("glove_grease_until", -1L).forGetter(Wellness::gloveGrease),
            Codec.FLOAT.optionalFieldOf("sun_min", 0f).forGetter(Wellness::sunMin),
            Codec.FLOAT.optionalFieldOf("wind_min", 0f).forGetter(Wellness::windMin),
            Codec.FLOAT.optionalFieldOf("ice_min", 0f).forGetter(Wellness::iceMin)
    ).apply(i, Wellness::new));

    // --- the whole record ----------------------------------------------------------------------------------
    public static final Codec<BodyState> CODEC = RecordCodecBuilder.create(i -> i.group(
            VITALS.forGetter(BodyCodec::vitals),
            BINS.forGetter(BodyCodec::bins),
            PARTS.forGetter(BodyCodec::parts),
            WELLNESS.forGetter(BodyCodec::wellness)
    ).apply(i, BodyCodec::assemble));

    static Vitals vitals(BodyState s) {
        return new Vitals(BodyState.SCHEMA, s.heightCm, s.sex, (float) s.kg, (float) s.stomachKg, (float) s.water,
                (float) s.warmth, (float) s.energy, s.awakeMin, s.asleep, s.sleepRun, (float) s.sleepQ, s.sos,
                (float) s.pendingKcal);
    }

    static Bins bins(BodyState s) {
        List<Float> wet = new ArrayList<>(Layer.SLOTS);
        for (double w : s.slotWet) wet.add((float) w);
        return new Bins(toInts(s.kcalIn), toInts(s.kcalOut), toInts(s.drunkMl), toInts(s.macroG), s.binHour,
                s.gutOnset, s.gutUntil, wet);
    }

    static Parts parts(BodyState s) {
        BodyRecord r = s.parts;
        return new Parts((float) r.condition(), (float) r.dSys(), toInts(r.partHp()), toInts(r.partFrost()),
                toInts(r.lostDigits()), r.crooked(), r.injurySeq(), r.problems());
    }

    static Wellness wellness(BodyState s) {
        List<Ill> ill = new ArrayList<>();
        for (var a : s.ill) ill.add(new Ill(a.id(), a.start(), a.until()));
        return new Wellness((float) s.vitC, (float) s.nausea, ill, s.restRun, (float) s.restWorkMin, s.restAwayMin,
                s.restSleepMin, s.restDay, (float) s.hairDays, s.facialHair, s.faceGreaseUntil, s.gloveGreaseUntil,
                (float) s.sunMin, (float) s.windMin, (float) s.iceMin);
    }

    static BodyState assemble(Vitals v, Bins b, Parts p, Wellness w) {
        BodyState s = new BodyState();
        s.heightCm = v.heightCm();
        s.sex = v.sex();
        s.kg = v.kg();
        s.stomachKg = v.stomachKg();
        s.water = v.water();
        s.warmth = v.warmth();
        s.energy = v.energy();
        s.awakeMin = v.awakeMin();
        s.asleep = v.asleep();
        s.sleepRun = v.sleepRun();
        s.sleepQ = v.sleepQ();
        s.sos = v.sos();
        s.pendingKcal = v.pendingKcal();

        fill(s.kcalIn, b.kcalIn());
        fill(s.kcalOut, b.kcalOut());
        fill(s.drunkMl, b.drunkMl());
        fill(s.macroG, b.macroG());
        s.binHour = b.binHour();
        s.gutOnset = b.gutOnset();
        s.gutUntil = b.gutUntil();
        for (int k = 0; k < Math.min(Layer.SLOTS, b.slotWet().size()); k++) s.slotWet[k] = b.slotWet().get(k);

        if (v.schema() < 2 || p.partHp().size() != Part.COUNT) {
            // Schema 1 → 2: every part 100, the old Condition loss all systemic, problems kept.
            s.parts = BodyRecord.upgradeFromSchema1(p.condition(), p.problems());
        } else {
            int[] hp = intArray(p.partHp(), Part.COUNT, 100);
            double dSys = p.dSys() >= 0 ? p.dSys() : 0;
            List<Problem> probs = p.problems().size() > BodyRecord.MAX_PROBLEMS
                    ? p.problems().subList(0, BodyRecord.MAX_PROBLEMS) : p.problems();
            s.parts = new BodyRecord(BodyRecord.SCHEMA, p.condition(), dSys, hp,
                    intArray(p.partFrost(), Part.COUNT, 0), intArray(p.lostDigits(), 2, 0), p.crooked(),
                    p.injurySeq(), probs);
        }
        // Schema 2 → 3: every new field takes its codec default (full vitamin C, no illness, hair at 0).
        s.vitC = w.vitC();
        s.nausea = w.nausea();
        for (Ill a : w.ill()) s.ill.add(new com.yearofice.sim.body.IllnessModel.Active(a.id(), a.start(), a.until()));
        s.restRun = w.restRun();
        s.restWorkMin = w.restWork();
        s.restAwayMin = w.restAway();
        s.restSleepMin = w.restSleep();
        s.restDay = w.restDay();
        s.hairDays = w.hairDays();
        s.facialHair = w.facialHair();
        s.faceGreaseUntil = w.faceGrease();
        s.gloveGreaseUntil = w.gloveGrease();
        s.sunMin = w.sunMin();
        s.windMin = w.windMin();
        s.iceMin = w.iceMin();
        return s;
    }

    /** Field-by-field equality of two records as saved (floats at save precision). */
    public static boolean sameAsSaved(BodyState a, BodyState b) {
        return vitals(a).equals(vitals(b)) && bins(a).equals(bins(b)) && parts(a).equals(parts(b))
                && wellness(a).equals(wellness(b));
    }
}
