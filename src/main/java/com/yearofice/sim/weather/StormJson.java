package com.yearofice.sim.weather;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/** Parses the weather data files that are not the climate: storms, rings and the forecast. */
public final class StormJson {
    private StormJson() {}

    public static double[] range(JsonObject o, String key, double a, double b) {
        if (!o.has(key)) return new double[] {a, b};
        JsonArray r = o.getAsJsonArray(key);
        return new double[] {r.get(0).getAsDouble(), r.get(1).getAsDouble()};
    }

    public static double[] months(JsonObject o, String key, double scale) {
        double[] m = new double[12];
        if (!o.has(key)) return m;
        JsonArray a = o.getAsJsonArray(key);
        for (int i = 0; i < 12; i++) m[i] = a.get(i).getAsDouble() * scale;
        return m;
    }

    public static StormDef storm(JsonObject o) {
        Map<String, Double> then = new HashMap<>();
        if (o.has("then")) for (var e : o.getAsJsonObject("then").entrySet()) then.put(e.getKey(), e.getValue().getAsDouble());
        return new StormDef(o.get("id").getAsString(), StormKind.of(o.get("kind").getAsString()),
                StormKind.PrecipKind.valueOf(o.get("precip_kind").getAsString().toUpperCase(java.util.Locale.ROOT)),
                months(o, "daily_start_pct", 0.01), range(o, "start_h", 0, 20), range(o, "duration_h", 2, 12),
                range(o, "intensity", 0.4, 1), range(o, "wind_ms", 0, 10), range(o, "precip_mm_h", 0, 0),
                range(o, "visibility_m", 10_000, 10_000), range(o, "gate_air_c", -99, 99),
                o.has("whiteout") && o.get("whiteout").getAsBoolean(),
                o.has("strike_every_min") ? o.get("strike_every_min").getAsDouble() : 0, Map.copyOf(then));
    }

    /** A ring: per-storm month multipliers (January first) over a default of 1. */
    public static Ring ring(JsonObject o) {
        Map<String, double[]> m = new HashMap<>();
        if (o.has("storm_mult")) for (var e : o.getAsJsonObject("storm_mult").entrySet()) {
            JsonElement v = e.getValue();
            double[] a = new double[12];
            if (v.isJsonArray()) for (int i = 0; i < 12; i++) a[i] = v.getAsJsonArray().get(i).getAsDouble();
            else java.util.Arrays.fill(a, v.getAsDouble());
            m.put(e.getKey(), a);
        }
        return new Ring(o.get("id").getAsString(), Map.copyOf(m));
    }

    public record Ring(String id, Map<String, double[]> stormMult) {
        public static final Ring NONE = new Ring("none", Map.of());

        public double mult(String stormId, int month) {
            double[] a = stormMult.get(stormId);
            return a == null ? 1 : a[month - 1];
        }
    }

    /** {@code weather/forecast.json}: what each sign says about the next 24 h, and the base rates. */
    public record Forecast(Map<String, Double> base, Map<String, Map<String, Double>> signs, double hit, double falseAlarm) {
        /** Noisy-or of the base rate with every sign seen, plus the tuning's bonus, per storm id. */
        public Map<String, Double> next24h(java.util.Collection<String> seen, double bonus) {
            Map<String, Double> out = new HashMap<>();
            for (var e : base.entrySet()) {
                double q = 1 - e.getValue();
                for (String s : seen) {
                    Double p = signs.getOrDefault(s, Map.of()).get(e.getKey());
                    if (p != null) q *= 1 - p;
                }
                out.put(e.getKey(), Math.max(0, Math.min(1, 1 - q + bonus * (1 - q))));
            }
            return out;
        }
    }

    public static Forecast forecast(JsonObject o) {
        Map<String, Double> base = new HashMap<>();
        for (var e : o.getAsJsonObject("base_24h").entrySet()) base.put(e.getKey(), e.getValue().getAsDouble());
        Map<String, Map<String, Double>> signs = new HashMap<>();
        for (var s : o.getAsJsonObject("signs").entrySet()) {
            Map<String, Double> m = new HashMap<>();
            for (var e : s.getValue().getAsJsonObject().entrySet()) m.put(e.getKey(), e.getValue().getAsDouble());
            signs.put(s.getKey(), Map.copyOf(m));
        }
        return new Forecast(Map.copyOf(base), Map.copyOf(signs), o.get("hit").getAsDouble(), o.get("false_alarm").getAsDouble());
    }
}
