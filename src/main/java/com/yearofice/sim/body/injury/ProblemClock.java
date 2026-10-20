package com.yearofice.sim.body.injury;

import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import java.util.ArrayList;
import java.util.List;

/** When a problem has run its course (Body tab › Problems by part; times in game minutes). */
public final class ProblemClock {
    private ProblemClock() {}

    private static final long DAY = 1440;

    public static boolean over(Problem p, long now) {
        long age = now - p.started();
        return switch (p.id()) {
            case Problems.BRUISE -> age >= 12 * 60;
            case Problems.SWELLING -> age >= 4 * 60;
            case Problems.HEAD_KNOCK -> age >= 24 * 60;
            case "back_strain" -> p.nextMark() >= 0 && now >= p.nextMark();
            case Problems.CUT -> p.stage() <= 1 && p.dressed() && age >= 3 * DAY;
            case Problems.BURN -> age >= (p.stage() >= 2 ? 4 : 2) * DAY / (p.cooled() ? 2 : 1);
            case Problems.SPRAIN -> p.wrap() >= 0 && now - p.wrap() >= (p.stage() >= 2 ? 5 : 2) * DAY;
            case Problems.BROKEN_BONE -> p.isSplinted() && now - p.splint() >= 30 * DAY;
            case Problems.BLISTERS -> p.dressed() && age >= DAY;
            default -> false;
        };
    }

    /** The record without the problems that ended by {@code now}. */
    public static BodyRecord expire(BodyRecord r, long now) {
        List<Problem> keep = new ArrayList<>();
        for (Problem p : r.problems()) if (!over(p, now)) keep.add(p);
        return keep.size() == r.problems().size() ? r : r.withProblems(keep);
    }
}
