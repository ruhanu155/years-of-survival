package com.yearofice.crop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yearofice.sim.time.Calendar;

/** One {@code data/yearofice/crop/<id>.json} row (Gson, so JUnit reads it without Minecraft). */
public final class CropSpec {
    public final String id, block, seedItem, yieldItem;
    public final double seedWeightG, baseGermination, savedGermination, sproutUnits, ripeUnits, sensFrom, sensTo,
            seedlingMinC, headMinC, headYieldMult, hardFreezeC, kgPerBlock, strawKg;
    public final int seedsPerBlock, windowFromDoy, windowToDoy;
    public final String standFormula;

    public CropSpec(JsonObject o) {
        id = o.get("id").getAsString();
        block = o.get("block").getAsString();
        JsonObject s = o.getAsJsonObject("seed");
        seedItem = s.get("item").getAsString();
        seedWeightG = s.get("weight_g").getAsDouble();
        baseGermination = s.get("base_germination").getAsDouble();
        savedGermination = s.get("saved_germination").getAsDouble();
        seedsPerBlock = s.get("seeds_per_block").getAsInt();
        JsonArray w = o.getAsJsonArray("sow_window");
        windowFromDoy = doy(w.get(0).getAsString());
        windowToDoy = doy(w.get(1).getAsString());
        JsonObject u = o.getAsJsonObject("units");
        sproutUnits = u.get("sprout").getAsDouble();
        ripeUnits = u.get("ripe").getAsDouble();
        sensFrom = u.getAsJsonArray("frost_sensitive").get(0).getAsDouble();
        sensTo = u.getAsJsonArray("frost_sensitive").get(1).getAsDouble();
        JsonObject f = o.getAsJsonObject("frost");
        seedlingMinC = f.get("seedling_min_c").getAsDouble();
        headMinC = f.get("head_min_c").getAsDouble();
        headYieldMult = f.get("head_yield_mult").getAsDouble();
        hardFreezeC = f.get("hard_freeze_c").getAsDouble();
        JsonObject y = o.getAsJsonObject("yield");
        yieldItem = y.get("item").getAsString();
        kgPerBlock = y.get("kg_per_block").getAsDouble();
        strawKg = y.get("straw_kg").getAsDouble();
        standFormula = y.get("stand_formula").getAsString();
    }

    /** "05-20" as a day of the year (0 = May 1). */
    static int doy(String md) {
        String[] p = md.split("-");
        return Calendar.dayOfYear(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    public boolean grain() { return standFormula.equals("tiller"); }

    public String name() { return id.substring(id.indexOf(':') + 1); }
}
