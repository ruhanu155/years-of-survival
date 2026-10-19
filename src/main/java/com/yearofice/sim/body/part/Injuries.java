package com.yearofice.sim.body.part;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.body.Zone;
import java.util.ArrayList;
import java.util.List;

/**
 * The injury path's pure core: what {@code BodyService.applyInjury} does to the record (Body tab › The
 * injury path, steps 2–4). The adapter ({@code body.injury.InjuryService}) reads the source file, calls
 * this, then posts {@code VitalChangedEvent}, the HUD line and {@code body_sync}, and tears the garment.
 *
 * <p>Reimplemented from understanding of First Aid's damage distribution (ichttt/FirstAid as forked by
 * maoruiQa/FIrst-Aid-New, GPL-3.0); no code copied. Kept from it: a data-driven table from damage source
 * to body parts, paired parts split between sides, parts that may not drop below a floor ("no-kill"
 * parts), and several distinct parts per event drawn without repeats. Changed to the Blueprint's rules:
 * one weighted roll instead of First Aid's ordered fill-then-overflow layers, overflow to the systemic
 * pool instead of to neighbouring parts, and a seeded {@link Mulberry32} in place of {@code java.util.Random}.
 *
 * <p>Roll order is fixed, so a seed replays exactly: parts first (one draw each), then the problem (one
 * draw), then its sub-place (one draw). The problem lands on the first hit part it can occur on (a 12 m
 * fall whose first part is the head puts its break on the next part); with none, only the loss applies.
 */
public final class Injuries {
    private Injuries() {}

    /** The outcome: the new record and what happened, for lines, the journal and the garment tear. */
    public record Result(BodyRecord record, List<Part> partsHit, Problem problem, boolean hitPack, Zone tearZone,
                         double tear) {}

    /** {@code applyInjury(source, record, rng)}: the source's own loss, standing, right-handed, no forced part. */
    public static BodyRecord applyInjury(InjurySource source, BodyRecord record, Mulberry32 rng) {
        return apply(source, record, rng, InjuryContext.DEFAULT).record();
    }

    public static BodyRecord applyInjury(InjurySource source, BodyRecord record, Mulberry32 rng, InjuryContext ctx) {
        return apply(source, record, rng, ctx).record();
    }

    /** The named stream for one injury: {@code hash(seed, player, "part", injury_seq)} as a Mulberry32 seed. */
    public static Mulberry32 rngFor(long worldSeed, String playerKey, int injurySeq) {
        return new Mulberry32(StageHash.of(worldSeed + ":" + playerKey + ":injury:" + injurySeq));
    }

    public static Result apply(InjurySource source, BodyRecord record, Mulberry32 rng, InjuryContext ctx) {
        int seq = record.injurySeq() + 1;
        double loss = Double.isNaN(ctx.lossOverride()) ? source.conditionLoss() : ctx.lossOverride();

        InjurySource.PostureWeights pw = source.posture().get(ctx.posture());
        if (pw != null && pw.firstHitPack() && ctx.packOn() && ctx.firstHitOfContact()) {
            BodyRecord r = record.with(record.condition(), record.dSys(), record.partHp(), seq, record.problems());
            return new Result(r, List.of(), null, true, null, 0);
        }

        if (source.systemic()) {
            BodyRecord r = PartModel.systemic(record, loss);
            r = r.with(r.condition(), r.dSys(), r.partHp(), seq, r.problems());
            return new Result(r, List.of(), null, false, null, 0);
        }

        List<Part> hits = rollParts(source, ctx, rng);
        BodyRecord r = record;
        if (hits.isEmpty()) {
            r = PartModel.systemic(r, loss);
        } else {
            double each = loss / hits.size();
            for (Part p : hits) r = PartModel.split(r, p, each);
        }

        Problem added = null;
        double u = rng.nextDouble();
        double v = rng.nextDouble();
        InjurySource.ProblemRoll roll = hits.isEmpty() ? null : pick(source.problems(), u);
        Part first = null;
        if (roll != null) {
            for (Part h : hits) {
                if (Problems.canLand(roll.id(), h)) {
                    first = h;
                    break;
                }
            }
        }
        if (first != null) {
            String sub = Problems.fixedSub(roll.id(), first);
            if (sub == null) sub = first.sub(Math.min(first.subCount() - 1, (int) (v * first.subCount())));
            added = Problem.fresh(roll.id(), first, sub, Math.max(1, roll.stage()), ctx.now());
        }
        List<Problem> problems = added == null ? r.problems() : addProblem(r.problems(), added);
        r = r.with(r.condition(), r.dSys(), r.partHp(), seq, problems);
        Zone tearZone = hits.isEmpty() || source.tearsOuter() <= 0 ? null : hits.get(0).zone;
        return new Result(r, List.copyOf(hits), added, false, tearZone, tearZone == null ? 0 : source.tearsOuter());
    }

    /** Picks {@code partsHit} distinct parts by weight; a forced part comes first. Always draws partsHit numbers. */
    static List<Part> rollParts(InjurySource source, InjuryContext ctx, Mulberry32 rng) {
        double[] w = source.weights(ctx.posture(), ctx.mainSide());
        List<Part> hits = new ArrayList<>(source.partsHit());
        if (ctx.forcedPart() != null) {
            hits.add(ctx.forcedPart());
            w[ctx.forcedPart().ordinal()] = 0;
        }
        while (hits.size() < source.partsHit()) {
            double u = rng.nextDouble();
            double total = 0;
            for (double x : w) total += x;
            if (total <= 0) break;
            double t = u * total, acc = 0;
            Part chosen = null;
            for (Part p : Part.values()) {
                if (w[p.ordinal()] <= 0) continue;
                acc += w[p.ordinal()];
                chosen = p;
                if (t < acc) break;
            }
            hits.add(chosen);
            w[chosen.ordinal()] = 0;
        }
        return hits;
    }

    /** Exclusive problem rolls: walk the cumulative chances; the first one the draw falls under wins. */
    static InjurySource.ProblemRoll pick(List<InjurySource.ProblemRoll> rolls, double u) {
        double acc = 0;
        for (InjurySource.ProblemRoll p : rolls) {
            acc += p.chance();
            if (u < acc) return p;
        }
        return null;
    }

    /**
     * Adds a problem and keeps triage order. At 16 problems, a new minor problem on a part that already has the
     * same one is folded into it (its timer restarts from the later start); any other 17th problem is dropped.
     */
    public static List<Problem> addProblem(List<Problem> list, Problem p) {
        List<Problem> l = new ArrayList<>(list);
        if (l.size() >= BodyRecord.MAX_PROBLEMS) {
            for (int i = 0; i < l.size(); i++) {
                Problem q = l.get(i);
                if (q.id().equals(p.id()) && q.part() == p.part() && Problems.isMinor(p)) {
                    l.set(i, q.withTimer(0, Math.max(q.started(), p.started()), q.nextMark()));
                    return Triage.sort(l);
                }
            }
            return List.copyOf(list);
        }
        l.add(p);
        return Triage.sort(l);
    }
}
