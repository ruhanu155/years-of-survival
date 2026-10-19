package com.yearofice.sim.body.part;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Triage order (Body tab › Several wounds at once): bleed rate, then time until worse ({@code next_mark},
 * soonest first), then effect size. Lists and the treat action both use it.
 */
public final class Triage {
    private Triage() {}

    public static final Comparator<Problem> ORDER = Comparator
            .comparingDouble((Problem p) -> -p.bleedPerMinute())
            .thenComparingLong(p -> p.nextMark() < 0 ? Long.MAX_VALUE : p.nextMark())
            .thenComparingInt(p -> -Problems.severity(p));

    /** A stable sort of the list into triage order. */
    public static List<Problem> sort(List<Problem> problems) {
        List<Problem> l = new ArrayList<>(problems);
        l.sort(ORDER);
        return List.copyOf(l);
    }

    /**
     * Treat all, for the bleeding wounds: dress each in triage order while bandages last.
     *
     * @return the dressed problems (in order), what is left, and whether it stopped for want of bandages
     */
    public record TreatAll(List<Problem> problems, List<Problem> treated, boolean ranOut, Problem stillBleeding) {}

    public static TreatAll dressBleeding(List<Problem> problems, int bandages, long now) {
        List<Problem> sorted = new ArrayList<>(sort(problems));
        List<Problem> treated = new ArrayList<>();
        int left = bandages;
        Problem stuck = null;
        for (int i = 0; i < sorted.size(); i++) {
            Problem p = sorted.get(i);
            if (p.bleedPerMinute() <= 0) continue;
            if (left <= 0) {
                stuck = p;
                break;
            }
            Problem d = p.dressed("bandage", "bandage", now + 12 * 60);
            sorted.set(i, d);
            treated.add(d);
            left--;
        }
        return new TreatAll(sort(sorted), List.copyOf(treated), stuck != null, stuck);
    }

    public static double totalBleed(List<Problem> problems) {
        double b = 0;
        for (Problem p : problems) b += p.bleedPerMinute();
        return b;
    }

    /** Shock: total bleeding of 4 a game minute or more, or 3 or more open wounds. */
    public static boolean shock(List<Problem> problems) {
        int open = 0;
        for (Problem p : problems) if (p.bleedPerMinute() > 0) open++;
        return totalBleed(problems) >= 4 || open >= 3;
    }
}
