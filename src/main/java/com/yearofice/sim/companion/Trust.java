package com.yearofice.sim.companion;

/**
 * A wolf's trust in one player (Open world in depth › Taming a wolf; Ideas from viewers › Taming a wolf without
 * bones). Each action has a minimum trust and a daily limit; the day's total gain is then clamped (+3 below 50,
 * +2 from 50 to 79, +1 from 80) so a pup needs at least 32 game days to reach Companion. A day with no gain is
 * neglect, and after the grace days trust drains.
 */
public final class Trust {
    public static final int TOLERANT = 25, BONDED = 50, COMPANION = 80;

    public enum Action {
        FEED_GROUND(2, 0, 3), HAND_FEED(3, TOLERANT, 1), SIT(1, 0, 2), ROUTINE(1, 0, 1), TALK(1, 0, 1),
        SHARE_FIRE(2, BONDED, 1), SHARE_KILL(5, 0, 99), PET(1, BONDED, 1), PLAY(1, BONDED, 1);

        final int gain, minTrust, limit;

        Action(int gain, int minTrust, int limit) {
            this.gain = gain;
            this.minTrust = minTrust;
            this.limit = limit;
        }
    }

    /** What the wolf is to the player: a pup that tolerates nothing yet, a juvenile that follows, a Companion. */
    public enum Stage { PUP, JUVENILE, COMPANION }

    public enum Rank { WARY, TOLERANT, BONDED, COMPANION }

    private double value, dayStart, today;
    private int neglect;
    private final int[] uses = new int[Action.values().length];

    public Trust(double start) {
        value = dayStart = Math.max(0, Math.min(100, start));
    }

    public double value() { return value; }

    public int neglectDays() { return neglect; }

    public double gainedToday() { return today; }

    public Stage stage() { return value >= COMPANION ? Stage.COMPANION : value >= TOLERANT ? Stage.JUVENILE : Stage.PUP; }

    public Rank rank() { return value >= COMPANION ? Rank.COMPANION : value >= BONDED ? Rank.BONDED : value >= TOLERANT ? Rank.TOLERANT : Rank.WARY; }

    public void set(double v) {
        value = Math.max(0, Math.min(100, v));
        dayStart = value;
        today = 0;
    }

    static double cap(double startOfDay, CompanionTuning t) {
        return startOfDay < BONDED ? t.capLow() : startOfDay < COMPANION ? t.capMid() : t.capHigh();
    }

    /** Applies one action; returns the trust it added (0 when its stage, its daily limit or the day's cap blocks it). */
    public double apply(Action a, CompanionTuning t) {
        int i = a.ordinal();
        if (value < a.minTrust || uses[i] >= a.limit) return 0;
        double room = cap(dayStart, t) - today;
        if (room <= 0) return 0;
        double g = Math.min(a.gain, room);
        uses[i]++;
        today += g;
        value = Math.min(100, value + g);
        return g;
    }

    /** A loss (stare -2, running at it -5, a strike -30, spoiled meat -3, killing wolves in its sight -10); no cap. */
    public void lose(double amount) { value = Math.max(0, value - amount); }

    /** Closes the day: neglect counts days with no gain and drains trust after the grace days. */
    public void endDay(CompanionTuning t) {
        if (today <= 0) neglect++;
        else neglect = 0;
        if (neglect > t.neglectGraceDays()) value = Math.max(0, value - t.neglectDecay());
        dayStart = value;
        today = 0;
        java.util.Arrays.fill(uses, 0);
    }

    /** value, day-start value, today's gain, neglect days, then each action's uses today. */
    public double[] save() {
        double[] d = new double[4 + uses.length];
        d[0] = value;
        d[1] = dayStart;
        d[2] = today;
        d[3] = neglect;
        for (int i = 0; i < uses.length; i++) d[4 + i] = uses[i];
        return d;
    }

    public static Trust load(double[] d) {
        Trust t = new Trust(d.length > 0 ? d[0] : 0);
        if (d.length >= 4) {
            t.dayStart = d[1];
            t.today = d[2];
            t.neglect = (int) d[3];
            for (int i = 0; i < t.uses.length && 4 + i < d.length; i++) t.uses[i] = (int) d[4 + i];
        }
        return t;
    }
}
