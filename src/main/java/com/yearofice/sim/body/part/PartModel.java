package com.yearofice.sim.body.part;

import java.util.Arrays;
import java.util.List;

/**
 * The part model's arithmetic (Body tab › Part health and Condition). Pure; no Minecraft types.
 *
 * <pre>
 *   Condition = 100 − D_sys − Σ_p s_p (100 − h_p)
 *   Δh_p = min(3L, h_p − floor_p),   ΔD_sys = L − s_p Δh_p
 *   f = R / (100 − Condition),   every pool scaled by (1 − f)
 * </pre>
 */
public final class PartModel {
    private PartModel() {}

    /** Part loss per Condition point ({@code body.part_gain}, 3 in every preset). */
    public static final double PART_GAIN = 3;
    /** The Endless return's systemic pool: Condition 80 with every part whole. */
    public static final double SOS_RETURN_D_SYS = 20;

    public static double condition(double dSys, int[] partHp) {
        double c = 100 - dSys;
        for (Part p : Part.values()) c -= p.share * (100 - partHp[p.ordinal()]);
        return Math.max(0, Math.min(100, c));
    }

    /**
     * The split rule for one event of loss {@code L} on part {@code p}: the part takes up to 3L, never below its
     * floor, in whole points; the pool takes the rest, so Condition falls by exactly L. A loss under a third of
     * a point moves only the pool, so ongoing local losses (infection stage 1, an unsplinted break) should be
     * applied in hourly or daily chunks.
     */
    public static BodyRecord split(BodyRecord r, Part p, double loss) {
        if (loss <= 0) return r;
        int[] hp = r.partHp();
        int h = hp[p.ordinal()];
        int dh = (int) Math.max(0, Math.min(Math.floor(PART_GAIN * loss + 1e-9), h - p.floor));
        hp[p.ordinal()] = h - dh;
        double dSys = Math.min(100, r.dSys() + loss - p.share * dh);
        return r.with(condition(dSys, hp), dSys, hp, r.injurySeq(), r.problems());
    }

    /** A loss with no part (bleeding, fever, starvation, hypothermia…): all systemic. */
    public static BodyRecord systemic(BodyRecord r, double loss) {
        if (loss <= 0) return r;
        double dSys = Math.min(100, r.dSys() + loss);
        int[] hp = r.partHp();
        return r.with(condition(dSys, hp), dSys, hp, r.injurySeq(), r.problems());
    }

    /**
     * C1 (RECONCILE 120): Condition heals unless a wound bleeds, an infection is active or warmth is under 30.
     * A splinted break or a dressed cut does not stop it.
     */
    public static boolean healingAllowed(List<Problem> problems, double warmth) {
        if (warmth < 30) return false;
        for (Problem p : problems) {
            if (p.bleedPerMinute() > 0) return false;
            if (Problems.INFECTION.equals(p.id()) || p.infected()) return false;
        }
        return true;
    }

    /**
     * One game hour of healing at rate {@code R} (Condition back per game hour, with its underweight, scurvy
     * and Rest-day factors already applied). Every pool shrinks by the same fraction. Parts round their
     * damage up, so a part is whole exactly when Condition is; the pool absorbs the rounding.
     */
    public static BodyRecord healHour(BodyRecord r, double rate) {
        double total = 100 - r.conditionFromParts();
        if (rate <= 0 || total <= 0) return r;
        if (rate >= total - 1e-9) {
            int[] hp = new int[Part.COUNT];
            Arrays.fill(hp, 100);
            return r.with(100, 0, hp, r.injurySeq(), r.problems());
        }
        double keep = 1 - rate / total;
        int[] hp = r.partHp();
        double partDamage = 0;
        for (Part p : Part.values()) {
            int dmg = 100 - hp[p.ordinal()];
            int kept = (int) Math.ceil(dmg * keep - 1e-9);
            hp[p.ordinal()] = 100 - kept;
            partDamage += p.share * kept;
        }
        double dSys = Math.max(0, (total - rate) - partDamage);
        return r.with(condition(dSys, hp), dSys, hp, r.injurySeq(), r.problems());
    }

    /** The Endless return: every part 100, the pool at 20 (Condition 80), problems and frost cleared; digits and crooked bones stay. */
    public static BodyRecord sosReturn(BodyRecord r) {
        int[] hp = new int[Part.COUNT];
        Arrays.fill(hp, 100);
        return new BodyRecord(BodyRecord.SCHEMA, condition(SOS_RETURN_D_SYS, hp), SOS_RETURN_D_SYS, hp,
                new int[Part.COUNT], r.lostDigits(), r.crooked(), r.injurySeq(), List.of());
    }

    /** {@code /yoi body part}: set one part's health and re-derive Condition. */
    public static BodyRecord setPart(BodyRecord r, Part p, int hp) {
        int[] h = r.partHp();
        h[p.ordinal()] = Math.max(p.floor, Math.min(100, hp));
        return r.with(condition(r.dSys(), h), r.dSys(), h, r.injurySeq(), r.problems());
    }

    /**
     * The band a part acts at for its own functions: its health band, one lower while an open deep cut, a
     * stage 1+ infection or a bruise is on it, and Hurt while quills are in it.
     */
    public static Band effectiveBand(BodyRecord r, Part p) {
        Band b = r.band(p);
        for (Problem pr : r.problems()) {
            if (pr.part() != p) continue;
            boolean lower = (Problems.CUT.equals(pr.id()) && pr.stage() >= 2 && !pr.dressed())
                    || (Problems.INFECTION.equals(pr.id()) && pr.stage() >= 1)
                    || (Problems.BRUISE.equals(pr.id()) && !p.isLimbOfGait());
            if (lower) b = b.worse(r.band(p).lower(p));
            if (Problems.QUILLS.equals(pr.id())) b = b.worse(Band.HURT);
        }
        return b;
    }
}
