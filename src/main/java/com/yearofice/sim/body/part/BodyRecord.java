package com.yearofice.sim.body.part;

import java.util.Arrays;
import java.util.List;

/**
 * The per-part slice of {@code yearofice:body} at schema 2 (Body tab › The body record, extended;
 * RECONCILE 121). Field names match the codec keys: {@code condition, d_sys, part_hp, part_frost,
 * lost_digits, crooked, injury_seq, problems}. The rest of the record (calories, water, warmth, energy…)
 * belongs to {@code sim.body.BodyModel}, which holds one of these. Immutable: every operation returns a new
 * record.
 *
 * <p>{@code part_hp} is whole numbers (a byte each in the save). The split and healing rules round the
 * part and let the systemic pool take the remainder, so {@code condition = 100 − d_sys − Σ s_p (100 − h_p)}
 * holds exactly at every step.
 *
 * @param lostDigits two shorts: [0] fingers (bits 0–4 left, little to thumb; 5–9 right), [1] toes, same layout
 * @param crooked    10 bits, one per part in {@link Part} order
 */
public record BodyRecord(int schema, double condition, double dSys, int[] partHp, int[] partFrost, int[] lostDigits,
                         int crooked, int injurySeq, List<Problem> problems) {

    public static final int SCHEMA = 2;
    public static final int MAX_PROBLEMS = 16;

    public BodyRecord {
        if (partHp.length != Part.COUNT || partFrost.length != Part.COUNT || lostDigits.length != 2) {
            throw new IllegalArgumentException("10 parts, 10 frost counters, 2 digit masks");
        }
        partHp = partHp.clone();
        partFrost = partFrost.clone();
        lostDigits = lostDigits.clone();
        problems = List.copyOf(problems);
    }

    /** A fresh body: every part 100, nothing wrong. Also the schema 1 → 2 upgrade's part values. */
    public static BodyRecord healthy() {
        int[] hp = new int[Part.COUNT];
        Arrays.fill(hp, 100);
        return new BodyRecord(SCHEMA, 100, 0, hp, new int[Part.COUNT], new int[2], 0, 0, List.of());
    }

    /** The schema 1 → 2 upgrade: every part 100, the old Condition loss all systemic, problems kept. */
    public static BodyRecord upgradeFromSchema1(double condition, List<Problem> problems) {
        BodyRecord h = healthy();
        return new BodyRecord(SCHEMA, condition, 100 - condition, h.partHp, h.partFrost, h.lostDigits, 0, 0, problems);
    }

    @Override public int[] partHp() { return partHp.clone(); }

    @Override public int[] partFrost() { return partFrost.clone(); }

    @Override public int[] lostDigits() { return lostDigits.clone(); }

    public int hp(Part p) { return partHp[p.ordinal()]; }

    public int frost(Part p) { return partFrost[p.ordinal()]; }

    public Band band(Part p) { return Band.of(hp(p)); }

    public boolean isCrooked(Part p) { return (crooked & (1 << p.ordinal())) != 0; }

    /** The Condition the parts and pool imply. The stored value must equal it. */
    public double conditionFromParts() { return PartModel.condition(dSys, partHp); }

    BodyRecord with(double condition, double dSys, int[] hp, int seq, List<Problem> problems) {
        return new BodyRecord(schema, condition, dSys, hp, partFrost, lostDigits, crooked, seq, problems);
    }

    public BodyRecord withProblems(List<Problem> p) {
        return new BodyRecord(schema, condition, dSys, partHp, partFrost, lostDigits, crooked, injurySeq, p);
    }

    public BodyRecord withFrost(int[] frost) {
        return new BodyRecord(schema, condition, dSys, partHp, frost, lostDigits, crooked, injurySeq, problems);
    }

    public BodyRecord withDigits(int[] digits) {
        return new BodyRecord(schema, condition, dSys, partHp, partFrost, digits, crooked, injurySeq, problems);
    }

    public BodyRecord withCrooked(int bits) {
        return new BodyRecord(schema, condition, dSys, partHp, partFrost, lostDigits, bits, injurySeq, problems);
    }

    @Override public boolean equals(Object o) {
        return o instanceof BodyRecord r && schema == r.schema && condition == r.condition && dSys == r.dSys
                && Arrays.equals(partHp, r.partHp) && Arrays.equals(partFrost, r.partFrost)
                && Arrays.equals(lostDigits, r.lostDigits) && crooked == r.crooked && injurySeq == r.injurySeq
                && problems.equals(r.problems);
    }

    @Override public int hashCode() {
        return Arrays.hashCode(partHp) * 31 + Double.hashCode(condition) * 17 + injurySeq;
    }

    @Override public String toString() {
        return "BodyRecord{condition=" + condition + ", d_sys=" + dSys + ", part_hp=" + Arrays.toString(partHp)
                + ", injury_seq=" + injurySeq + ", problems=" + problems.size() + "}";
    }
}
