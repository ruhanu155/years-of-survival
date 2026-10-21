package com.yearofice.sim.life;

import com.yearofice.sim.decay.Thresholds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The year's calendar of arrivals and departures (Birds and the air › The migration calendar; Waters and fish › The
 * water calendar): which waves are on, which species are in the valley on a day, how loud the sky is. A species draws
 * its arrival within plus or minus {@code jitter} days of its mean, from (seed, species, year), so a replay draws the
 * same spring. Pure Java. Original Year of Ice code.
 */
public final class MigrationCalendar {
    private final List<MigrationDef> defs;

    public MigrationCalendar(Collection<MigrationDef> defs) {
        List<MigrationDef> l = new ArrayList<>(defs);
        l.sort((a, b) -> Integer.compare(a.from(), b.from()));
        this.defs = List.copyOf(l);
    }

    public List<MigrationDef> all() { return defs; }

    /** Waves and runs on today, with the breakup shift applied. */
    public List<MigrationDef> active(int doy, double breakupEarlyDays) {
        List<MigrationDef> out = new ArrayList<>();
        for (MigrationDef d : defs) if (d.shifted(breakupEarlyDays).active(doy)) out.add(d.shifted(breakupEarlyDays));
        return out;
    }

    /** The per-year jitter of a species' mean day, -jitter to +jitter. */
    public static int jitter(long seed, String species, int year, double jitterDays) {
        int j = (int) Math.round(jitterDays);
        if (j <= 0) return 0;
        return Thresholds.stream(seed, "migration", species.hashCode(), year).nextInt(2 * j + 1) - j;
    }

    /**
     * Is the species in the valley on {@code doy}? Presence windows come from the data; the species' arrival is moved
     * by its yearly jitter and, for a wave that shifts with spring warmth, by the breakup. A species with no
     * presence row is always present (the spawner's month rule decides).
     */
    public boolean present(String species, int doy, long seed, int year, double breakupEarlyDays, double jitterDays) {
        boolean known = false;
        for (MigrationDef d : defs) {
            for (MigrationDef.Presence p : d.presences()) {
                if (!p.species().equals(species)) continue;
                known = true;
                int j = jitter(seed, species, year, jitterDays);
                int shift = d.shifts() ? (int) -Math.round(5.0 * breakupEarlyDays / 7.0) : 0;
                if (doy >= p.from() + j + shift && doy <= p.to() + j) return true;
            }
        }
        return !known;
    }

    /** How busy the sky is: the strongest bird wave today, 0 to 1 (the peak weeks of waves 3 and 6 pass 4 to 12 flocks a day). */
    public double skyTraffic(int doy, double breakupEarlyDays) {
        double s = 0;
        for (MigrationDef d : defs) if (d.kind().equals("bird_wave")) s = Math.max(s, d.shifted(breakupEarlyDays).strength(doy));
        return s;
    }

    /** Sky flocks passing over on a day: 0 outside waves 3 and 6, up to 12 at their peak. */
    public int flocksToday(int doy, double breakupEarlyDays) {
        int best = 0;
        for (MigrationDef d : defs) {
            if (!d.kind().equals("bird_wave") || !(d.id().endsWith("duck_flood") || d.id().endsWith("big_pass"))) continue;
            best = Math.max(best, (int) Math.round(4 + 8 * d.shifted(breakupEarlyDays).strength(doy)) * (d.shifted(breakupEarlyDays).active(doy) ? 1 : 0));
        }
        return best;
    }
}
