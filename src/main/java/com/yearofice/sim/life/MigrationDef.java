package com.yearofice.sim.life;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;

/**
 * One {@code data/yearofice/migration/<id>.json}: a window of the year when something arrives, passes or leaves.
 * Dates are written {@code "MM-DD"} and stored as game-year days (May 1 is day 0, {@link Calendar}).
 *
 * @param kind      {@code bird_wave}, {@code caribou}, {@code salmon_run}, {@code fish_run} or {@code lamprey_run}
 * @param shifts    true when spring warmth moves the wave (waves 2 and 3: breakup 7 days early pulls them 5 days forward)
 * @param run       for a run, the {@code SalmonRun.RUNS} key it follows
 * @param nights    for the lamprey run, the least and most nights the run lasts inside its window
 * @param presences species present from a date to a date ({@code species} is a path; {@code from} and {@code to} are days)
 * @param arrivals  species that arrive in the window, each at its mean day (a draw of plus or minus jitter days is added)
 */
public record MigrationDef(String id, String kind, int from, int peak, int to, boolean shifts, String run, int[] nights,
                           String line, int spirits, List<Presence> presences, List<Arrival> arrivals) {

    public record Presence(String species, int from, int to) {}

    public record Arrival(String species, int day) {}

    public static int day(String mmdd) {
        String[] p = mmdd.split("-");
        return Calendar.dayOfYear(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    public static MigrationDef parse(JsonObject o) {
        List<Presence> pres = new ArrayList<>();
        if (o.has("present")) for (JsonElement e : o.getAsJsonArray("present")) {
            JsonObject p = e.getAsJsonObject();
            pres.add(new Presence(p.get("species").getAsString(), day(p.get("from").getAsString()), day(p.get("to").getAsString())));
        }
        List<Arrival> arr = new ArrayList<>();
        if (o.has("arrivals")) for (JsonElement e : o.getAsJsonArray("arrivals")) {
            JsonObject p = e.getAsJsonObject();
            arr.add(new Arrival(p.get("species").getAsString(), day(p.get("date").getAsString())));
        }
        int[] nights = o.has("nights") ? new int[] {o.getAsJsonArray("nights").get(0).getAsInt(), o.getAsJsonArray("nights").get(1).getAsInt()} : new int[0];
        int from = day(o.get("from").getAsString()), to = day(o.get("to").getAsString());
        int peak = o.has("peak") ? day(o.get("peak").getAsString()) : (from + to) / 2;
        return new MigrationDef(o.get("id").getAsString(), o.get("kind").getAsString(), from, peak, to,
                o.has("shifts") && o.get("shifts").getAsBoolean(), o.has("run") ? o.get("run").getAsString() : "", nights,
                o.has("line") ? o.get("line").getAsString() : "", o.has("spirits") ? o.get("spirits").getAsInt() : 0,
                List.copyOf(pres), List.copyOf(arr));
    }

    /** The window as drawn for a year: warmth moves a shifting wave (5 days per 7 days of early breakup). */
    public MigrationDef shifted(double breakupEarlyDays) {
        if (!shifts) return this;
        int d = (int) -Math.round(5.0 * breakupEarlyDays / 7.0);
        return new MigrationDef(id, kind, from + d, peak + d, to + d, shifts, run, nights, line, spirits, presences, arrivals);
    }

    public boolean active(int doy) { return doy >= from && doy <= to; }

    /** Triangle across the window: 0 outside, 1 on the peak. */
    public double strength(int doy) {
        if (!active(doy)) return 0;
        double s = doy <= peak ? (doy - from + 1.0) / (peak - from + 1.0) : (to - doy + 1.0) / (to - peak + 1.0);
        return Math.max(0, Math.min(1, s));
    }
}
