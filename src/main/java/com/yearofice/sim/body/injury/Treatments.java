package com.yearofice.sim.body.injury;

import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Injuries;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.PartModel;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import java.util.ArrayList;
import java.util.List;

/**
 * The pure treatments (Body tab › Treatments by part; Ideas from viewers › Cauterizing and grime). Each returns a
 * new record, or the same one when the treatment does not fit. The adapter checks items and holds and posts the
 * Messenger line.
 */
public final class Treatments {
    private Treatments() {}

    public record Cauterized(BodyRecord record, boolean applied, boolean blackout, double conditionLoss) {}

    /** The index of the first problem of {@code id} on {@code part} (any part when null), or −1. */
    public static int find(List<Problem> ps, String id, Part part) {
        for (int i = 0; i < ps.size(); i++) {
            Problem p = ps.get(i);
            if (p.id().equals(id) && (part == null || p.part() == part)) return i;
        }
        return -1;
    }

    /** Dress one problem with a pad and binding, good for 12 game hours (6 on a back wound). */
    public static BodyRecord bandage(BodyRecord r, int index, long now) {
        List<Problem> l = new ArrayList<>(r.problems());
        Problem p = l.get(index);
        long hours = p.part() == Part.TORSO && "back".equals(p.sub()) ? 6 : 12;
        l.set(index, p.dressed("bandage", "bandage", now + hours * 60));
        return r.withProblems(com.yearofice.sim.body.part.Triage.sort(l));
    }

    /** A splint starts the 30-day timer and stops the unsplinted loss. */
    public static BodyRecord splint(BodyRecord r, int index, long now) {
        List<Problem> l = new ArrayList<>(r.problems());
        l.set(index, l.get(index).splinted(now));
        return r.withProblems(l);
    }

    public static BodyRecord sling(BodyRecord r, int index) {
        List<Problem> l = new ArrayList<>(r.problems());
        l.set(index, l.get(index).withSling(true));
        return r.withProblems(l);
    }

    public static BodyRecord sprainWrap(BodyRecord r, int index, long now) {
        List<Problem> l = new ArrayList<>(r.problems());
        l.set(index, l.get(index).wrapped(now));
        return r.withProblems(l);
    }

    /**
     * Cauterize the cut at {@code index}: bleeding stops at once; the cut becomes a bad burn (an open wound for 4
     * days), Condition −{@code loss} (8), and below {@code blackoutBelow} Condition a blackout is possible
     * ({@code blackoutDraw} under {@code blackoutChance}). Only a deep cut qualifies.
     */
    public static Cauterized cauterize(BodyRecord r, int index, long now, double loss, double blackoutBelow,
                                       double blackoutChance, double blackoutDraw) {
        Problem cut = r.problems().get(index);
        if (!Problems.CUT.equals(cut.id()) || cut.stage() < 2) return new Cauterized(r, false, false, 0);
        List<Problem> l = new ArrayList<>(r.problems());
        l.set(index, Problem.fresh(Problems.BURN, cut.part(), cut.sub(), 2, now));
        BodyRecord out = r.withProblems(com.yearofice.sim.body.part.Triage.sort(l));
        out = PartModel.split(out, cut.part(), loss);
        boolean blackout = out.condition() < blackoutBelow && blackoutDraw < blackoutChance;
        return new Cauterized(out, true, blackout, loss);
    }

    /** Cooling a burn within 10 game minutes halves recovery (written); marks it. */
    public static BodyRecord cool(BodyRecord r, int index, long now, int windowMinutes) {
        Problem p = r.problems().get(index);
        if (!Problems.BURN.equals(p.id()) || now - p.started() > windowMinutes) return r;
        List<Problem> l = new ArrayList<>(r.problems());
        Problem c = p;
        l.set(index, new Problem(c.id(), c.part(), c.sub(), c.stage(), c.timer(), c.dressed(), c.infected(), c.pad(),
                c.binding(), c.cleanUntil(), c.splint(), c.sling(), c.wrap(), true, c.started(), c.nextMark()));
        return r.withProblems(l);
    }

    /** Removes a problem outright (quills pulled, leech off, a treated bruise). */
    public static BodyRecord remove(BodyRecord r, int index) {
        List<Problem> l = new ArrayList<>(r.problems());
        l.remove(index);
        return r.withProblems(l);
    }

    /**
     * One step of the problems clock: Condition lost to bleeding over {@code minutes} (systemic), scaled by the
     * bleed dial. A dressing that has run out ({@code cleanUntil} passed) stops covering.
     */
    public static BodyRecord bleed(BodyRecord r, int minutes, double bleedScale, long now) {
        double loss = 0;
        for (Problem p : r.problems()) loss += p.bleedPerMinute();
        if (loss <= 0 || minutes <= 0) return r;
        return PartModel.systemic(r, loss * minutes * bleedScale);
    }

    /** Add a problem (used by back strain and the debug command). */
    public static BodyRecord add(BodyRecord r, Problem p) {
        return r.withProblems(Injuries.addProblem(r.problems(), p));
    }
}
