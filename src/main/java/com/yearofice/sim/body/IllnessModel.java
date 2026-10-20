package com.yearofice.sim.body;

import java.util.ArrayList;
import java.util.List;

/**
 * The illness engine (Body tab › Sickness register; Blueprint › Health, medicine and nutrition). The list of
 * {@link Active} entries lives in {@link BodyState#ill}; the shapes live in {@link IllnessDefs}. An entry shows
 * its effects from {@code start} (the end of the incubation) until {@code until} (−1 until cured), the stage
 * following the hours since {@code start}. Scurvy is the one illness the model starts itself, from the vitamin C
 * reserve; the rest are started by whoever causes them through {@link #begin}.
 *
 * <p>Pure and deterministic: no clocks, no randomness. Callers hand in the roll ({@code frac}, 0–1) for an onset
 * inside a window. Effects fold in list order, but every fold is a product, a minimum or a sum, so the list order
 * (kept sorted anyway) never changes a result.
 */
public final class IllnessModel {
    private IllnessModel() {}

    /** One illness in the list; {@code start} may be in the future while it incubates. */
    public record Active(String id, long start, long until) {}

    /** The folded effects of everything showing at one minute. */
    public record Effects(double absorb, double waterLDay, double energyCap, double condPerDay, double nauseaPerH,
                          double nauseaFloor, double condCap, double heal, double burn, double walk, double chillC) {
        public static final Effects NONE = new Effects(1, 0, 10, 0, 0, 0, 100, 1, 1, 1, 0);
    }

    public static final int GUT_NAUSEA_HOURS = 12;
    public static final double GUT_NAUSEA_PER_H = 15;
    /** C5: bed, drinking and remedies never shorten a gut illness below this many game hours. */
    public static final int GUT_FLOOR_HOURS = 12;
    public static final double VITC_FULL_DAYS = 30;

    public static boolean showing(Active a, long m) { return a.start() <= m && (a.until() < 0 || m < a.until()); }

    public static boolean active(BodyState s, String id, long m) {
        for (Active a : s.ill) if (a.id().equals(id) && showing(a, m)) return true;
        return false;
    }

    public static boolean listed(BodyState s, String id) {
        for (Active a : s.ill) if (a.id().equals(id)) return true;
        return false;
    }

    /** Puts an illness on the list at {@code start} until {@code until}, replacing a listed one (a restart). */
    public static void start(BodyState s, String id, long start, long until) {
        s.ill.removeIf(a -> a.id().equals(id));
        s.ill.add(new Active(id, start, until));
        sort(s.ill);
    }

    private static void sort(List<Active> l) {
        l.sort((a, b) -> a.start() != b.start() ? Long.compare(a.start(), b.start()) : a.id().compareTo(b.id()));
    }

    /**
     * Starts a registered illness from its file: the onset is {@code frac} of the way through its incubation
     * window, the end its duration times the preset's illness scale. Returns false for an unknown id or one
     * stepped by another lane.
     */
    public static boolean begin(BodyState s, String id, long now, double frac, WellnessTuning w) {
        IllnessDef d = IllnessDefs.get(id);
        if (d == null || !d.steppedHere()) return false;
        long onset = now + Math.round((d.onsetMinH() + frac * (d.onsetMaxH() - d.onsetMinH())) * 60);
        long until = d.durationH() < 0 ? -1 : onset + Math.round(d.durationH() * 60 * w.illnessScale());
        start(s, id, onset, until);
        return true;
    }

    public static boolean cure(BodyState s, String id) { return s.ill.removeIf(a -> a.id().equals(id)); }

    /** Hours since the entry showed; negative while it incubates. */
    public static double hours(Active a, long m) { return (m - a.start()) / 60.0; }

    /** The folded effects of every illness showing at minute {@code m}, and the gut illness. */
    public static Effects effects(BodyState s, long m) {
        double absorb = 1, water = 0, cap = 10, cond = 0, nPerH = 0, nFloor = 0, condCap = 100, heal = 1, burn = 1,
                walk = 1, chill = 0;
        if (s.gutIll(m)) {
            absorb *= Metabolism.GUT_ILL_FACTOR;
            if (gutHours(s, m) < GUT_NAUSEA_HOURS) nPerH += GUT_NAUSEA_PER_H;
        }
        for (Active a : s.ill) {
            if (!showing(a, m)) continue;
            IllnessDef d = IllnessDefs.get(a.id());
            if (d == null) continue;
            double h = hours(a, m);
            IllnessDef.Stage st = d.stageAt(h);
            if (st == null) continue;
            absorb *= st.absorb();
            water += st.waterLDay();
            cap = Math.min(cap, st.energyCap());
            cond += st.condPerDay();
            if (st.nauseaForH() == 0 || h < st.nauseaForH()) nPerH += st.nauseaPerH();
            nFloor = Math.max(nFloor, st.nauseaFloor());
            condCap = Math.min(condCap, st.condCap());
            heal *= st.heal();
            // Scurvy's burn and slowness end the day the reserve is above 0; its caps and slow healing last 7 days.
            boolean fixed = "scurvy".equals(a.id()) && s.vitC > 0;
            if (!fixed) {
                burn *= st.burn();
                walk *= st.walk();
            }
            chill += chillNow(st, a, m);
        }
        return new Effects(absorb, water, cap, cond, nPerH, nFloor, condCap, heal, burn, walk, chill);
    }

    /** Fever's chills: the last hour of every 3-hour cycle (hot 2 h, chills 1 h). */
    static double chillNow(IllnessDef.Stage st, Active a, long m) {
        if (st.chillC() == 0) return 0;
        long into = Math.floorMod(m - a.start(), 180);
        return into >= 120 ? st.chillC() : 0;
    }

    /** Hours into the current gut illness, or 0 when well. The illness starts {@code GUT_ILL_MINUTES} before its end. */
    public static double gutHours(BodyState s, long m) {
        if (s.gutUntil < 0) return 0;
        return (m - (s.gutUntil - Metabolism.GUT_ILL_MINUTES)) / 60.0;
    }

    /** The product of the gut factors on food eaten at {@code m} (RECONCILE: 1.0, 0.5 gut, 0.85 parasites, 0.8 lean). */
    public static double absorb(BodyState s, long m) { return effects(s, m).absorb(); }

    /** Shortens a gut illness by {@code hours} (bed, fluids, geranium, buckbean), never below the floor (C5). */
    public static void easeGut(BodyState s, long m, double hours) {
        if (s.gutUntil < 0) return;
        long start = s.gutUntil - Metabolism.GUT_ILL_MINUTES;
        long floor = Math.max(start, m) + 0;
        long earliest = Math.max(floor, start + GUT_FLOOR_HOURS * 60L);
        s.gutUntil = Math.max(earliest, s.gutUntil - Math.round(hours * 60));
    }

    /** Pushes the nausea meter (poison plants, fermented berries, tea, rancid fat); at 100 the next step vomits. */
    public static void addNausea(BodyState s, double points) { s.nausea = Math.max(0, Math.min(100, s.nausea + points)); }

    /** Vitamin C from food: reserve days, capped at 30. */
    public static void addVitC(BodyState s, double days) { s.vitC = Math.min(VITC_FULL_DAYS, s.vitC + Math.max(0, days)); }

    /** One chunk of {@code n} minutes from {@code m}: expiry, stage lines, scurvy, lean-meat, the nausea meter. */
    public static void tick(BodyState s, BodyTuning t, WellnessTuning w, long m, int n, boolean asleep) {
        long end = m + n;
        Effects ef = effects(s, m);

        // Vitamin C and scurvy: day 1 at an empty reserve.
        double before = s.vitC;
        s.vitC = Math.max(0, s.vitC - n / 1440.0);
        if (s.vitC <= 0) {
            Active sc = find(s, "scurvy");
            if (sc == null) {
                long at = before > 0 ? m + Math.round(before * 1440) : m;
                s.ill.add(new Active("scurvy", Math.min(at, end), -1));
                sort(s.ill);
                if (at <= m) event(s, "ill_scurvy_1");
            } else if (sc.until() >= 0) {
                replace(s, sc, new Active("scurvy", sc.start(), -1));
            }
        } else {
            Active sc = find(s, "scurvy");
            if (sc != null && sc.until() < 0 && sc.start() <= m) {
                IllnessDef d = IllnessDefs.get("scurvy");
                replace(s, sc, new Active("scurvy", sc.start(), end + (d == null ? 0 : d.recoveryH() * 60L)));
            }
        }

        // Stage lines (messenger explanations), before expiry so the last stage still speaks.
        for (Active a : s.ill) {
            IllnessDef d = IllnessDefs.get(a.id());
            if (d == null) continue;
            int i0 = a.start() > m ? -1 : d.stageIndex(hours(a, m));
            int i1 = a.start() > end ? -1 : d.stageIndex(hours(a, end));
            if (i1 > i0 && i1 >= 0) {
                String msg = d.stages().get(i1).msg();
                if (!msg.isEmpty()) event(s, msg);
            }
        }
        List<Active> done = new ArrayList<>();
        for (Active a : s.ill) if (a.until() >= 0 && a.until() <= end) done.add(a);
        for (Active a : done) {
            s.ill.remove(a);
            event(s, "ill_" + a.id() + "_end");
        }

        // Lean-meat sickness, checked as each hour closes.
        if (Math.floorMod(end, 60) == 0) leanMeat(s, end);

        // Nausea: a meter pushed to 100 (poison, tea, rancid fat) vomits at once; then sources minus the decay.
        if (s.nausea >= 100) vomit(s, t, m);
        double decay = asleep ? 15 : 10;
        s.nausea = Math.max(0, Math.min(100, s.nausea + (ef.nauseaPerH() - decay) * n / 60.0));
        s.nausea = Math.max(s.nausea, Math.min(99, ef.nauseaFloor()));
        if (s.nausea >= 100) vomit(s, t, m);
    }

    private static void leanMeat(BodyState s, long end) {
        double c = 0, f = 0, p = 0;
        for (int h = 0; h < BodyState.MACRO_HOURS; h++) {
            c += s.macroG[h * 3 + Metabolism.CARBS];
            f += s.macroG[h * 3 + Metabolism.FAT];
            p += s.macroG[h * 3 + Metabolism.PROTEIN];
        }
        double kcal = 4 * c + 9 * f + 4 * p;
        if (kcal < 3 * 1200) return; // three days of real eating, not a fast
        double fat = 9 * f / kcal, protein = 4 * p / kcal;
        boolean on = listed(s, "lean_meat");
        if (!on && fat < 0.30 && protein > 0.45) {
            s.ill.add(new Active("lean_meat", end, -1));
            sort(s.ill);
        } else if (on && fat >= 0.60) {
            cure(s, "lean_meat");
            event(s, "ill_lean_meat_end");
        }
    }

    /** A vomit: half the last three hours' calories and half a litre of water go; the meter falls back to 20. */
    static void vomit(BodyState s, BodyTuning t, long m) {
        int hour = (int) Math.floorMod(Math.floorDiv(m, 60), 24);
        double lost = 0;
        for (int k = 0; k < 3; k++) {
            int b = Math.floorMod(hour - k, 24);
            double half = s.kcalIn[b] * 0.5;
            lost += half;
            s.kcalIn[b] -= half;
        }
        s.kg -= lost / t.kcalPerKg();
        s.stomachKg *= 0.5;
        s.water = Math.max(0, s.water - Metabolism.waterPoints(0.5));
        s.nausea = 20;
        event(s, "vomit");
    }

    private static Active find(BodyState s, String id) {
        for (Active a : s.ill) if (a.id().equals(id)) return a;
        return null;
    }

    private static void replace(BodyState s, Active old, Active next) {
        int i = s.ill.indexOf(old);
        if (i >= 0) s.ill.set(i, next);
    }

    static void event(BodyState s, String id) {
        if (s.events.size() < 32) s.events.add(id);
    }
}
