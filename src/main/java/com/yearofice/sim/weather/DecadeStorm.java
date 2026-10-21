package com.yearofice.sim.weather;

import com.google.gson.JsonObject;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;

/** The storm of the decade (Weather and storms › Storm of the decade): rolls on May 1, at most one in any {@code minGapYears}. */
public record DecadeStorm(double chancePerYear, int fromYear, int minGapYears, List<Variant> variants) {

    /** {@code storm} names the {@link StormDef} it plays; the window is days of the year, 0 = May 1. */
    public record Variant(String id, double weight, String storm, int fromDoy, int toDoy, int hours, double intensity) {}

    public record Draw(Variant variant, int year, long startHour) {}

    public static DecadeStorm parse(JsonObject o) {
        List<Variant> v = new ArrayList<>();
        for (var e : o.getAsJsonArray("variants")) {
            JsonObject j = e.getAsJsonObject();
            v.add(new Variant(j.get("id").getAsString(), j.get("weight_pct").getAsDouble(), j.get("storm").getAsString(),
                    doy(j.getAsJsonArray("window").get(0).getAsString()), doy(j.getAsJsonArray("window").get(1).getAsString()),
                    j.get("duration_h").getAsInt(), j.get("intensity").getAsDouble()));
        }
        return new DecadeStorm(o.get("chance_per_year").getAsDouble(), o.get("from_year").getAsInt(), o.get("min_gap_years").getAsInt(), List.copyOf(v));
    }

    /** "MM-DD" as a day of the year. */
    static int doy(String mmdd) { return Calendar.dayOfYear(Integer.parseInt(mmdd.substring(0, 2)), Integer.parseInt(mmdd.substring(3))); }

    /** Every year in 1..{@code upToYear} the storm lands, in order, with the cooldown applied. */
    public List<Draw> simulate(long seed, int upToYear) {
        List<Draw> out = new ArrayList<>();
        int last = Integer.MIN_VALUE / 2;
        for (int y = Math.max(1, fromYear); y <= upToYear; y++) {
            if (y - last < minGapYears) continue;
            Mulberry32 r = new Mulberry32(StageHash.of(seed + ":decade:" + y));
            if (r.nextDouble() >= chancePerYear) continue;
            double total = variants.stream().mapToDouble(Variant::weight).sum(), pick = r.nextDouble() * total;
            Variant v = variants.get(variants.size() - 1);
            for (Variant c : variants) {
                if ((pick -= c.weight()) < 0) { v = c; break; }
            }
            int lastStart = Math.max(v.fromDoy(), v.toDoy() - v.hours() / 24);
            int day = v.fromDoy() + r.nextInt(lastStart - v.fromDoy() + 1);
            out.add(new Draw(v, y, ((long) (y - 1) * Calendar.DAYS_PER_YEAR + day) * 24 + 6));
            last = y;
        }
        return out;
    }

    public Draw drawFor(long seed, int year) {
        for (Draw d : simulate(seed, year)) if (d.year() == year) return d;
        return null;
    }
}
