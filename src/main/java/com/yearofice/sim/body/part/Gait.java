package com.yearofice.sim.body.part;

/**
 * Walking from the legs and feet (Body tab › Bands and part effects, How parts combine, Limping and the
 * walking stick). Each side uses the worse of its leg and foot; on one part the worst effect wins; across
 * sides walk = min(left, right), × 0.9 when both are below 1, never under 0.25.
 */
public final class Gait {
    private Gait() {}

    public static final double MIN_WALK = 0.25;
    /** Splinted legs ease to 80% from day 11 (Health › Broken bones). */
    public static final int SPLINT_EASE_DAYS = 11;

    public record Limp(Side side, int severity) {
        public static final Limp NONE = new Limp(Side.NONE, 0);
    }

    /** One part's walking factor from its band and every gait problem on it. */
    public static double partWalk(BodyRecord r, Part p, long now, boolean stick) {
        double f = PartModel.effectiveBand(r, p).walk();
        if (stick) f = 1 - (1 - f) / 2; // band losses on the worse side are halved by the stick
        for (Problem pr : r.problems()) {
            if (pr.part() != p) continue;
            f = Math.min(f, problemWalk(pr, now, stick));
        }
        if (r.isCrooked(p)) f *= 0.95;
        return f;
    }

    static double problemWalk(Problem pr, long now, boolean stick) {
        return switch (pr.id()) {
            case Problems.SPRAIN -> stick ? 0.75 : 0.5;
            case Problems.BLISTERS -> stick ? 1 - 0.15 / 2 : 0.85;
            case Problems.BROKEN_BONE -> {
                boolean eased = pr.isSplinted() && now - pr.splint() >= SPLINT_EASE_DAYS * 1440L;
                double base = eased ? 0.8 : 0.6;
                yield stick ? (eased ? 0.9 : 0.7) : base;
            }
            case Problems.BRUISE -> stick ? 0.95 : 0.9;
            case Problems.BURN -> 0.8;
            case Problems.FROSTBITE -> pr.stage() >= 3 ? 0.7 : (pr.stage() == 2 ? 0.85 : 1);
            default -> 1;
        };
    }

    public static double side(BodyRecord r, Side s, long now, boolean stick) {
        Part leg = Part.of(Part.Group.LEGS, s), foot = Part.of(Part.Group.FEET, s);
        return Math.min(partWalk(r, leg, now, stick), partWalk(r, foot, now, stick));
    }

    /**
     * The {@code yearofice:injury} walk-speed factor. {@code stick} is a walking stick in the off hand, which
     * helps the worse side.
     */
    public static double walk(BodyRecord r, long now, boolean stick) {
        double left = side(r, Side.LEFT, now, false), right = side(r, Side.RIGHT, now, false);
        if (stick) {
            if (left <= right) left = side(r, Side.LEFT, now, true);
            else right = side(r, Side.RIGHT, now, true);
        }
        double w = Math.min(left, right);
        if (left < 1 && right < 1) w *= 0.9;
        return Math.max(MIN_WALK, w);
    }

    /** The limp: shown when the sides differ by 0.1 or more, or any leg or foot problem has a gait effect. */
    public static Limp limp(BodyRecord r, long now) {
        double left = side(r, Side.LEFT, now, false), right = side(r, Side.RIGHT, now, false);
        boolean gaitProblem = false;
        for (Problem pr : r.problems()) {
            if (pr.part().isLimbOfGait() && problemWalk(pr, now, false) < 1) gaitProblem = true;
        }
        if (Math.abs(left - right) < 0.1 && !gaitProblem) return Limp.NONE;
        double worse = Math.min(left, right);
        Side s = left < right ? Side.LEFT : (right < left ? Side.RIGHT : Side.NONE);
        int sev = worse >= 0.8 ? 1 : (worse >= 0.5 ? 2 : 3);
        return new Limp(s, sev);
    }
}
