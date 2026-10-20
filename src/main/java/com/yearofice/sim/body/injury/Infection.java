package com.yearofice.sim.body.injury;

import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Injuries;
import com.yearofice.sim.body.part.PartModel;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Infection and the slow losses (Body tab › Infection; Blueprint › Cuts and dressings, Infection). Pure.
 * A cut left undressed for 12 game hours becomes an infection on its part; a dressing that has run out rolls 15%
 * every 6 game hours. An infection is stage 1 for its first day (−10 Condition a day, local), stage 2 until it is 84
 * hours old (fever and blackouts, lane B's illness half), then stage 3 (blood poisoning, −20 a day, systemic). A
 * cleaned and redressed stage 1 does not advance. A break left unsplinted loses 5 a day, local.
 */
public final class Infection {
    private Infection() {}

    public static final long DAY = 1440;
    public static final long UNDRESSED_MIN = 12 * 60, DIRTY_ROLL_MIN = 6 * 60, STAGE2_AT = DAY, STAGE3_AT = 84 * 60;
    public static final double DIRTY_CHANCE = 0.15, STAGE1_PER_DAY = 10, STAGE3_PER_DAY = 20, UNSPLINTED_PER_DAY = 5;

    /** The stage an infection of {@code ageMinutes} has reached. */
    public static int stageAt(long ageMinutes) {
        return ageMinutes >= STAGE3_AT ? 3 : ageMinutes >= STAGE2_AT ? 2 : 1;
    }

    /** Deterministic 0..1 roll for a problem and a 6-hour bucket (no RNG state to save). */
    static double roll(Problem p, long bucket) {
        return new SplittableRandom(p.started() * 31 + p.part().ordinal() * 1_000_003L + bucket * 7919L + p.id().hashCode())
                .nextDouble();
    }

    private static boolean hasInfection(List<Problem> ps, Problem cut) {
        for (Problem q : ps) if (Problems.INFECTION.equals(q.id()) && q.part() == cut.part()) return true;
        return false;
    }

    /**
     * Advances infections and slow losses over {@code [start, start + minutes)}. {@code scale} is the bleeding and
     * infection speed dial (×0.5 Gentle, ×1 Standard, ×1.5 Brutal). Local losses go in at each game-day boundary
     * (whole parts points), the systemic stage-3 loss runs by the minute.
     */
    public static BodyRecord advance(BodyRecord r, long start, int minutes, double scale) {
        if (minutes <= 0 || r.problems().isEmpty()) return r;
        long end = start + minutes;
        List<Problem> ps = new ArrayList<>(r.problems());
        // New infections.
        for (Problem p : List.copyOf(ps)) {
            if (!Problems.CUT.equals(p.id()) || hasInfection(ps, p)) continue;
            boolean infect = false;
            long at = end;
            if (!p.dressed() && end - p.started() >= UNDRESSED_MIN) {
                infect = true;
                at = Math.max(start, p.started() + UNDRESSED_MIN);
            } else if (p.dressed() && p.cleanUntil() >= 0 && end > p.cleanUntil()) {
                long first = Math.max(p.cleanUntil(), start);
                for (long b = (first + DIRTY_ROLL_MIN - 1) / DIRTY_ROLL_MIN; b * DIRTY_ROLL_MIN <= end; b++) {
                    if (roll(p, b) < DIRTY_CHANCE * Math.min(1, scale)) {
                        infect = true;
                        at = b * DIRTY_ROLL_MIN;
                        break;
                    }
                }
            }
            if (infect) ps = new ArrayList<>(Injuries.addProblem(ps, Problem.fresh(Problems.INFECTION, p.part(), p.sub(), 1, at)));
        }
        BodyRecord out = r.withProblems(ps);
        // Stages and losses.
        long dayCrossings = end / DAY - start / DAY;
        double sysLoss = 0;
        ps = new ArrayList<>(out.problems());
        for (int i = 0; i < ps.size(); i++) {
            Problem p = ps.get(i);
            if (Problems.INFECTION.equals(p.id())) {
                int stage = stageAt(end - p.started());
                if (p.dressed() && p.stage() == 1) stage = 1; // cleaned and redressed: stage 1 stops advancing
                else if (stage < p.stage()) stage = p.stage();
                if (stage != p.stage()) {
                    p = p.withStage(stage);
                    ps.set(i, p);
                }
                if (p.stage() == 1 && dayCrossings > 0) out = PartModel.split(out.withProblems(ps), p.part(), STAGE1_PER_DAY * scale * dayCrossings);
                if (p.stage() >= 3) sysLoss += STAGE3_PER_DAY * scale * minutes / DAY;
            } else if (Problems.BROKEN_BONE.equals(p.id()) && !p.isSplinted() && dayCrossings > 0) {
                out = PartModel.split(out.withProblems(ps), p.part(), UNSPLINTED_PER_DAY * dayCrossings);
            }
            out = out.withProblems(ps);
        }
        out = out.withProblems(ps);
        return sysLoss > 0 ? PartModel.systemic(out, sysLoss) : out;
    }
}
