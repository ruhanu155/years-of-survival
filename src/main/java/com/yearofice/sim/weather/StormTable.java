package com.yearofice.sim.weather;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongToDoubleFunction;

/**
 * One year of storms (Weather and storms › Deterministic and replayable): a daily Markov chain over the storm
 * definitions, every roll from a stream named by seed, day and storm id, never by call order. A storm that starts
 * runs its drawn hours; when it ends each of its {@code then} followers may start at once; otherwise the next day
 * rolls again. The decade storm and forced debug storms are laid on top.
 */
public final class StormTable {
    public record Event(String id, StormKind kind, long startHour, int hours, double intensity, boolean forced) {
        public long endHour() { return startHour + hours; }

        public boolean covers(long absHour) { return absHour >= startHour && absHour < startHour + hours; }
    }

    static final int YEAR_HOURS = Calendar.DAYS_PER_YEAR * 24;
    private final int year;
    private final List<Event> events;

    private StormTable(int year, List<Event> events) {
        this.year = year;
        this.events = events;
    }

    public int year() { return year; }

    public List<Event> events() { return events; }

    /** The strongest storm covering {@code absHour}, or null. */
    public Event at(long absHour) {
        Event best = null;
        for (Event e : events) if (e.covers(absHour) && (best == null || e.intensity() > best.intensity())) best = e;
        return best;
    }

    /** A stable hash over every event, for the replay test. */
    public int hash() {
        StringBuilder sb = new StringBuilder();
        for (Event e : events) sb.append(e.id()).append(',').append(e.startHour()).append(',').append(e.hours()).append(',')
                .append(Math.round(e.intensity() * 1000)).append(';');
        return StageHash.of(sb.toString());
    }

    public static StormTable build(long seed, int year, Collection<StormDef> defs, StormJson.Ring ring, double frequency,
                                   LongToDoubleFunction dayMeanC, DecadeStorm decade, Collection<Event> forced) {
        List<StormDef> sorted = defs.stream().sorted(Comparator.comparing(StormDef::id)).toList();
        java.util.Map<String, StormDef> byId = new java.util.HashMap<>();
        for (StormDef d : sorted) byId.put(d.id(), d);
        long y0 = (long) (year - 1) * YEAR_HOURS, y1 = y0 + YEAR_HOURS;
        List<Event> out = new ArrayList<>();
        long busy = y0;
        for (int d = 0; d < Calendar.DAYS_PER_YEAR; d++) {
            long absDay = (long) (year - 1) * Calendar.DAYS_PER_YEAR + d;
            if (absDay * 24 + 23 < busy) continue;
            int month = Calendar.month(d);
            StormDef pick = null;
            double bestScore = Double.MAX_VALUE;
            double mean = Double.NaN;
            for (StormDef def : sorted) {
                double p = def.chance(month) * ring.mult(def.id(), month) * frequency;
                if (p <= 0) continue;
                if (Double.isNaN(mean)) mean = dayMeanC.applyAsDouble(absDay);
                if (mean < def.tempC()[0] || mean > def.tempC()[1]) continue;
                double roll = new Mulberry32(StageHash.of(seed + ":storm:" + absDay + ":" + def.id())).nextDouble();
                if (roll < p && roll / p < bestScore) {
                    bestScore = roll / p;
                    pick = def;
                }
            }
            if (pick == null) continue;
            long start = Math.max(busy, absDay * 24 + hourOf(seed, absDay, pick));
            Event e = event(seed, absDay, pick, start, y1);
            if (e == null) continue;
            out.add(e);
            busy = e.endHour();
            // the Markov step: followers start where the storm ends
            StormDef cur = pick;
            for (int depth = 0; depth < 3 && busy < y1; depth++) {
                StormDef next = null;
                for (var t : new java.util.TreeMap<>(cur.then()).entrySet()) {
                    StormDef nd = byId.get(t.getKey());
                    if (nd == null || nd.chance(Calendar.month((int) ((busy / 24) % Calendar.DAYS_PER_YEAR))) <= 0) continue; // a follower still needs its season
                    double roll = new Mulberry32(StageHash.of(seed + ":then:" + busy + ":" + t.getKey())).nextDouble();
                    if (roll < t.getValue() * frequency) { next = nd; break; }
                }
                if (next == null) break;
                Event f = event(seed, busy / 24, next, busy, y1);
                if (f == null) break;
                out.add(f);
                busy = f.endHour();
                cur = next;
            }
        }
        if (decade != null) {
            DecadeStorm.Draw dr = decade.drawFor(seed, year);
            if (dr != null && byId.containsKey(dr.variant().storm())) {
                StormDef def = byId.get(dr.variant().storm());
                out.add(new Event(dr.variant().storm(), def.kind(), dr.startHour(), dr.variant().hours(), dr.variant().intensity(), false));
            }
        }
        for (Event f : forced) if (f.startHour() < y1 && f.endHour() > y0) out.add(f);
        out.sort(Comparator.comparingLong(Event::startHour).thenComparing(Event::id));
        return new StormTable(year, List.copyOf(out));
    }

    private static long hourOf(long seed, long absDay, StormDef def) {
        double u = new Mulberry32(StageHash.of(seed + ":hour:" + absDay + ":" + def.id())).nextDouble();
        return (long) Math.floor(StormDef.lerp(def.startHour(), u));
    }

    private static Event event(long seed, long absDay, StormDef def, long start, long yearEnd) {
        Mulberry32 r = new Mulberry32(StageHash.of(seed + ":params:" + absDay + ":" + def.id() + ":" + start));
        int hours = (int) Math.round(StormDef.lerp(def.durationH(), r.nextDouble()));
        double intensity = StormDef.lerp(def.intensity(), r.nextDouble());
        hours = (int) Math.min(hours, yearEnd - start); // ponytail: a storm ends at New Year — upgrade: carry the tail into the next table
        return hours < 1 ? null : new Event(def.id(), def.kind(), start, hours, intensity, false);
    }
}
