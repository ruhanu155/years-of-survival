package com.yearofice.sim.livestock;

import com.google.gson.JsonObject;

/**
 * One {@code data/yearofice/livestock/<id>.json} (Raising animals). Pure numbers; the Minecraft side only loads the file.
 * Calendar months are 1–12. A species with no milk has {@code milkLPerDay} 0; no eggs, {@code eggsPerDayMax} 0.
 */
public record Spec(String id, double adultKg, double birthKg, double matureDays, double weanDays, double feedKgPerDay,
                   double waterLPerDay, double comfortC, double coldSlopePerC, double fedDecayPerDay, double waterDecayPerDay,
                   double troughFeedDays, double troughWaterDays, int birthMonth, int rutFromMonth, int rutToMonth,
                   double milkLPerDay, int lactationDays, double hayOnlyFactor, double eggsPerDayMax,
                   double hairKgPerYear, double lifespanYears) {

    public static Spec parse(JsonObject o) {
        JsonObject n = obj(o, "needs"), c = obj(o, "calendar"), p = obj(o, "produce"), g = obj(o, "growth");
        return new Spec(o.get("entity").getAsString(), num(g, "adult_kg", 1), num(g, "birth_kg", 0.05), num(g, "mature_days", 120),
                num(g, "wean_days", 30), num(n, "feed_kg_per_d", 0.1), num(n, "water_l_per_d", 0.1), num(n, "comfort_c", 0),
                num(n, "cold_slope_per_c", 5), num(n, "fed_decay_per_d", 80), num(n, "water_decay_per_d", 100),
                num(n, "trough_feed_days", 3), num(n, "trough_water_days", 2), (int) num(c, "birth_month", 5),
                (int) num(c, "rut_from_month", 10), (int) num(c, "rut_to_month", 11), num(p, "milk_l_per_d", 0),
                (int) num(p, "lactation_days", 0), num(p, "hay_only_factor", 0.75), num(p, "eggs_per_d_max", 0),
                num(p, "hair_kg_per_year", 0), num(o, "lifespan_years", 10));
    }

    static JsonObject obj(JsonObject o, String k) { return o.has(k) ? o.getAsJsonObject(k) : new JsonObject(); }

    static double num(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
}
