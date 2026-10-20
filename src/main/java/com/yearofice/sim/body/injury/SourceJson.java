package com.yearofice.sim.body.injury;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.body.part.Posture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads one {@code injury_source/<id>.json} into the pure {@link InjurySource} (Body tab › Data paths). Keys are
 * snake_case: {@code condition_loss}, {@code parts}, {@code posture}, {@code problems}, {@code tears_outer},
 * {@code parts_hit}, {@code systemic}. No Minecraft imports.
 */
public final class SourceJson {
    private SourceJson() {}

    public static InjurySource parse(String id, JsonObject o) {
        double loss = o.has("condition_loss") ? o.get("condition_loss").getAsDouble() : 0;
        int partsHit = o.has("parts_hit") ? o.get("parts_hit").getAsInt() : 1;
        Map<String, Double> parts = weights(o.has("parts") ? o.getAsJsonObject("parts") : null);
        Map<Posture, InjurySource.PostureWeights> posture = new LinkedHashMap<>();
        if (o.has("posture")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("posture").entrySet()) {
                JsonObject pw = e.getValue().getAsJsonObject();
                boolean pack = pw.has("first_hit") && "pack".equals(pw.get("first_hit").getAsString());
                posture.put(Posture.valueOf(e.getKey().toUpperCase(Locale.ROOT)),
                        new InjurySource.PostureWeights(pack, weights(pw.getAsJsonObject("parts"))));
            }
        }
        List<InjurySource.ProblemRoll> rolls = new ArrayList<>();
        if (o.has("problems")) {
            JsonArray a = o.getAsJsonArray("problems");
            for (JsonElement el : a) {
                JsonObject p = el.getAsJsonObject();
                rolls.add(new InjurySource.ProblemRoll(p.get("id").getAsString(),
                        p.has("stage") ? p.get("stage").getAsInt() : 1, p.get("chance").getAsDouble()));
            }
        }
        double tears = o.has("tears_outer") ? o.get("tears_outer").getAsDouble() : 0;
        boolean systemic = o.has("systemic") && o.get("systemic").getAsBoolean();
        return new InjurySource(id, loss, partsHit, parts, posture, rolls, tears, systemic);
    }

    private static Map<String, Double> weights(JsonObject o) {
        Map<String, Double> m = new LinkedHashMap<>();
        if (o != null) for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), e.getValue().getAsDouble());
        return m;
    }
}
