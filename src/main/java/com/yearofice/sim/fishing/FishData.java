package com.yearofice.sim.fishing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads one {@code data/yearofice/fish/<id>.json} (Blueprint › Fishing in depth › Species data) into the sim's
 * records, so no species is hard-coded. Every key is snake_case; a missing required key throws. Pure Java (Gson
 * only). Original Year of Ice code.
 *
 * @param netCPerHour gill-net c at full coverage, q = 1 and a full stock
 * @param capacity    spot type to the species' capacity at q = 1
 */
public record FishData(FishSpecies species, FishSeason season, String parasite, double netCPerHour, String mesh,
                       Map<String, Double> capacity, double rPerMonth) {

    public static final String[] REQUIRED = {"id", "kg", "bite_base", "kcal_per_kg", "run_pull_x", "run_every_s",
            "run_length_s", "jump_chance", "special", "tire_per_s", "bite_through_no_leader", "night_factor", "bait",
            "season", "resident", "parasite", "net_c_per_h", "mesh", "capacity", "r_per_month"};

    public static FishData parse(JsonObject o) {
        for (String k : REQUIRED) if (!o.has(k)) throw new IllegalArgumentException(idOf(o) + ": missing " + k);
        for (String k : o.keySet()) if (!k.equals(k.toLowerCase(Locale.ROOT)) || k.contains("-")) throw new IllegalArgumentException(idOf(o) + ": key not snake_case: " + k);
        String id = o.get("id").getAsString();
        double[] kg = arr(o, "kg");
        FishSpecies.Special sp = FishSpecies.Special.valueOf(o.get("special").getAsString().toUpperCase(Locale.ROOT));
        double[] every = o.has("special_every_s") && !o.get("special_every_s").isJsonNull() ? arr(o, "special_every_s") : null;
        double[] trophy = o.has("trophy_kg") && !o.get("trophy_kg").isJsonNull() ? arr(o, "trophy_kg") : null;
        Map<String, Double> bait = new TreeMap<>();
        o.getAsJsonObject("bait").entrySet().forEach(e -> bait.put(e.getKey(), e.getValue().getAsDouble()));
        FishSpecies s = new FishSpecies(id, kg[0], kg[1], o.get("bite_base").getAsDouble(), o.get("kcal_per_kg").getAsDouble(),
                o.get("run_pull_x").getAsDouble(), arr(o, "run_every_s"), arr(o, "run_length_s"), o.get("jump_chance").getAsDouble(),
                sp, every, o.has("special_spike") ? o.get("special_spike").getAsDouble() : 1.0, o.get("tire_per_s").getAsDouble(),
                o.get("bite_through_no_leader").getAsDouble(), o.get("night_factor").getAsDouble(), trophy, bait,
                o.has("surface_feeder") && o.get("surface_feeder").getAsBoolean());
        JsonObject se = o.getAsJsonObject("season");
        boolean all = se.has("all_year") && se.get("all_year").getAsBoolean();
        int from = all ? 1 : date(se.get("from").getAsString()), to = all ? 365 : date(se.get("to").getAsString());
        JsonObject sw = o.has("spawn") && o.get("spawn").isJsonObject() ? o.getAsJsonObject("spawn") : null;
        FishSeason season = new FishSeason(from, to, all, o.get("resident").getAsBoolean(), sw == null ? null : sw.get("site").getAsString(),
                sw == null ? 0 : date(sw.get("from").getAsString()), sw == null ? 0 : date(sw.get("to").getAsString()));
        Map<String, Double> cap = new TreeMap<>();
        o.getAsJsonObject("capacity").entrySet().forEach(e -> cap.put(e.getKey(), e.getValue().getAsDouble()));
        return new FishData(s, season, o.get("parasite").getAsString(), o.get("net_c_per_h").getAsDouble(),
                o.get("mesh").getAsString(), Map.copyOf(cap), o.get("r_per_month").getAsDouble());
    }

    /** "MM-DD" to the game-year day. */
    static int date(String md) {
        String[] p = md.split("-");
        return FishSeason.day(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    private static double[] arr(JsonObject o, String k) {
        JsonArray a = o.getAsJsonArray(k);
        double[] r = new double[a.size()];
        for (int i = 0; i < r.length; i++) { JsonElement e = a.get(i); r[i] = e.getAsDouble(); }
        return r;
    }

    private static String idOf(JsonObject o) { return o.has("id") ? o.get("id").getAsString() : "?"; }

    /** Capacity of this species at a spot type at q (0 if the type does not hold it). */
    public double capacityAt(String spotType, double q) { return capacity.getOrDefault(spotType, 0.0) * q; }
}
