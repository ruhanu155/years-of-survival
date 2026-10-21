package com.yearofice.sim.world;

/**
 * Degree-day ice for one water body class (Minecraft engine › Ice, snow and permafrost; Life under the ice).
 * Frost degree-days pile up while the body is open; ice starts when the class's onset is reached, grows by
 * Stefan's law (slowed by snow lying on it), melts by positive degree-days and rain, turns rotten after losing a
 * share of its peak, and a river's rotten ice breaks up (which raises the flood pulse). Pure Java.
 */
public final class IceModel {
    private IceModel() {}

    public enum BodyClass { RIVER, LAKE, POND, SEA_EDGE }

    /** What the block shows; thickness itself lives in the {@link State}. */
    public enum Stage {
        OPEN, SKIM, THIN, SAFE, ROTTEN;

        /** The next weaker state, for thin-spot columns. */
        public Stage weaker() { return this == SAFE || this == ROTTEN ? THIN : this == THIN ? SKIM : this == SKIM ? OPEN : this; }
    }

    public enum Event { NONE, FREEZE_UP, BREAKUP, MELT_OUT }

    /** Mutable per-class ice record. {@code cm} 0 means open water. */
    public static final class State {
        public double cm, fdd, peakCm;
        public boolean rotten;

        public State copy() {
            State s = new State();
            s.cm = cm; s.fdd = fdd; s.peakCm = peakCm; s.rotten = rotten;
            return s;
        }
    }

    /** One day. {@code snowCm} lies on the ice; {@code rainMm} falls as rain. */
    public static Event step(State s, BodyClass c, IceTuning t, double meanC, double rainMm, double snowCm) {
        if (s.cm <= 0) {
            if (meanC < 0) s.fdd += -meanC;
            else s.fdd = Math.max(0, s.fdd - meanC * t.warmDecay());
            if (s.fdd >= t.onset(c) && meanC < 0) {
                s.cm = t.skimCm();
                s.peakCm = s.cm;
                s.rotten = false;
                return Event.FREEZE_UP;
            }
            return Event.NONE;
        }
        if (meanC < 0) {
            double a = t.growth(c);
            double insul = 1.0 / (1.0 + Math.max(0, snowCm) / t.snowInsulCm());
            s.cm += a * a * (-meanC) / (2 * Math.max(s.cm, 1.0)) * insul;
            if (s.cm >= s.peakCm) {
                s.peakCm = s.cm;
                s.rotten = false;
            }
        } else {
            s.cm -= t.meltCmPerPdd() * meanC + (meanC > 0.5 ? t.rainMeltCmPerMm() * rainMm : 0);
            if (s.peakCm > 0 && (s.peakCm - s.cm) / s.peakCm * 100 >= t.rottenLossPct() && s.peakCm >= t.thinCm()) s.rotten = true;
        }
        // a river sheet that rots and goes in one warm spell is still a breakup (the flood pulse), never a quiet melt-out
        if (c == BodyClass.RIVER && s.rotten && s.cm <= t.breakupCmRiver()) {
            s.cm = 0; s.fdd = 0; s.peakCm = 0; s.rotten = false;
            return Event.BREAKUP;
        }
        if (s.cm <= 0) {
            s.cm = 0; s.fdd = 0; s.peakCm = 0; s.rotten = false;
            return Event.MELT_OUT;
        }
        return Event.NONE;
    }

    /** Thickness that counts for strength: rotten ice counts as {@code rottenStrength} of its thickness. */
    public static double effectiveCm(State s, IceTuning t) { return s.rotten ? s.cm * t.rottenStrength() : s.cm; }

    public static Stage stage(State s, IceTuning t) {
        if (s.cm <= 0) return Stage.OPEN;
        if (s.cm < 2) return Stage.SKIM;
        if (s.rotten) return Stage.ROTTEN;
        return s.cm < t.thinCm() ? Stage.THIN : Stage.SAFE;
    }

    /** Safe ice holds a walker at the thin line and a loaded sled at {@code sledCm}; heavier loads need 1.5x that. */
    public static boolean holds(double effectiveCm, double loadKg, IceTuning t) {
        double need = loadKg <= 150 ? t.thinCm() : loadKg <= 400 ? t.sledCm() : t.sledCm() * 1.5;
        return effectiveCm >= need;
    }

    /** Game minutes an ice hole takes to skin over at {@code airC}; infinite above the cut-off. */
    public static double holeSkinMinutes(double airC, IceTuning t) {
        if (airC >= t.holeSkinAboveC()) return Double.POSITIVE_INFINITY;
        if (airC <= -20) return t.holeSkinMinAtM20();
        return t.holeSkinMinAtM20() * Math.pow(-20.0 / airC, t.holeSkinExp());
    }
}
