package com.yearofice.sim.hunt;

import com.google.gson.JsonObject;

/** An arrowhead (data {@code arrowhead/<id>.json}): energy factor, shatter chance, reuse, bleed factor. */
public record ArrowHead(String id, double energyFactor, double shatterOnBone, double reuse, double bleedFactor, boolean blunt, boolean passesShoulder) {

    public static ArrowHead parse(JsonObject o) {
        return new ArrowHead(o.get("id").getAsString(), o.get("energy_factor").getAsDouble(), o.get("shatter_on_bone").getAsDouble(),
                o.get("reuse").getAsDouble(), o.get("bleed_factor").getAsDouble(), o.get("blunt").getAsBoolean(),
                o.has("passes_shoulder") && o.get("passes_shoulder").getAsBoolean());
    }
}
