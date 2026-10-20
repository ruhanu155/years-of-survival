package com.yearofice.sim.body;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * One file of {@code data/yearofice/illness/<id>.json} (Body tab › Sickness register). The file holds the shapes:
 * incubation, duration and the effects of each stage by hours since onset. The dials that scale them live in
 * {@code yearofice-common.toml [body]}. Pure Java: Gson only.
 *
 * <pre>
 * { "id": "yearofice:scurvy", "engine": "body", "source": "...", "onset_h": [0, 0], "duration_h": -1,
 *   "recovery_h": 168, "stages": [ { "from_h": 0, "cap_bolts": 8, "heal": 0.5, "msg": "ill_scurvy_1" } ] }
 * </pre>
 *
 * {@code engine} is {@code body} when this lane steps the illness, or the owning lane's name when another system
 * starts, steps and cures it (the file is then the register entry and the explanation).
 *
 * @param durationH  game hours the illness runs once it shows; −1 until cured
 * @param recoveryH  game hours the effects last after the cause is fixed (scurvy 168)
 */
public record IllnessDef(String id, String engine, String source, int onsetMinH, int onsetMaxH, int durationH,
                         int recoveryH, List<Stage> stages) {

    /**
     * The effects from {@code fromH} hours after onset. Multipliers default to 1, caps to 10 (energy) and 100
     * (Condition). {@code nauseaForH} 0 means the nausea source runs while the stage does.
     */
    public record Stage(int fromH, double absorb, double waterLDay, double energyCap, double condPerDay,
                        double nauseaPerH, int nauseaForH, double nauseaFloor, double condCap, double heal,
                        double burn, double walk, double chillC, String msg) {
        public static final Stage NONE = new Stage(0, 1, 0, 10, 0, 0, 0, 0, 100, 1, 1, 1, 0, "");
    }

    public boolean steppedHere() { return "body".equals(engine); }

    /** The stage in force {@code hours} after onset (the last whose {@code from_h} has passed), or null. */
    public Stage stageAt(double hours) {
        Stage found = null;
        for (Stage s : stages) if (hours >= s.fromH()) found = s;
        return found;
    }

    public int stageIndex(double hours) {
        int idx = -1;
        for (int i = 0; i < stages.size(); i++) if (hours >= stages.get(i).fromH()) idx = i;
        return idx;
    }

    public static IllnessDef parse(JsonObject o) {
        String id = o.get("id").getAsString();
        JsonArray on = o.has("onset_h") ? o.getAsJsonArray("onset_h") : new JsonArray();
        int lo = on.size() > 0 ? on.get(0).getAsInt() : 0;
        int hi = on.size() > 1 ? on.get(1).getAsInt() : lo;
        List<Stage> stages = new ArrayList<>();
        if (o.has("stages")) {
            for (JsonElement e : o.getAsJsonArray("stages")) {
                JsonObject s = e.getAsJsonObject();
                stages.add(new Stage(i(s, "from_h", 0), d(s, "absorb", 1), d(s, "water_daily_l", 0),
                        d(s, "cap_bolts", 10), d(s, "cond_daily", 0), d(s, "nausea_per_h", 0),
                        i(s, "nausea_for_h", 0), d(s, "nausea_floor", 0), d(s, "cond_cap", 100), d(s, "heal", 1),
                        d(s, "burn", 1), d(s, "walk", 1), d(s, "chill_c", 0),
                        s.has("msg") ? s.get("msg").getAsString() : ""));
            }
        }
        stages.sort((a, b) -> Integer.compare(a.fromH(), b.fromH()));
        return new IllnessDef(id, o.has("engine") ? o.get("engine").getAsString() : "body",
                o.has("source") ? o.get("source").getAsString() : "", lo, hi, i(o, "duration_h", -1),
                i(o, "recovery_h", 0), List.copyOf(stages));
    }

    /** The id without the namespace: {@code yearofice:scurvy} is {@code scurvy}. */
    public String shortId() { return id.substring(id.indexOf(':') + 1); }

    private static double d(JsonObject o, String k, double def) { return o.has(k) ? o.get(k).getAsDouble() : def; }

    private static int i(JsonObject o, String k, int def) { return o.has(k) ? o.get(k).getAsInt() : def; }
}
