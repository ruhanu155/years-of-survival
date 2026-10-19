package com.yearofice.sim.fire;

/**
 * One armload (about 15 kg) of one wood at one moisture in a fire's queue. {@code amount} is the share of an
 * armload left, 0–1, so four 3.75 kg split pieces make 1.0 and a part-burned load keeps its remainder.
 */
public final class Armload {
    public final Wood wood;
    public final int moisturePct;
    private double amount;

    public Armload(Wood wood, int moisturePct, double amount) {
        if (wood == null) throw new IllegalArgumentException("wood");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.wood = wood;
        this.moisturePct = Math.max(0, Math.min(100, moisturePct));
        this.amount = amount;
    }

    public double amount() { return amount; }

    /** Burns up to {@code share} of an armload; returns what was left over (share not burned). */
    double burn(double share) {
        double used = Math.min(amount, share);
        amount -= used;
        return share - used;
    }

    void add(double more) { amount += more; }

    public boolean spent() { return amount <= 1e-9; }

    /** Green or soaked wood: 50% moisture or more (Blueprint › Seasoning: green wood is 50%). */
    public boolean green() { return moisturePct >= FireRules.GREEN_MOISTURE; }

    /** Two loads merge when wood and moisture band match. */
    boolean sameKind(Wood w, int m) { return wood == w && moisturePct == m; }
}
