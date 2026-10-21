package com.yearofice.story.radio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** {@code data/yearofice/radio/<name>.json} read from the jar (ponytail: fixed names, no reload listener). */
public final class RadioData {
    private RadioData() {}

    private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();

    public static JsonObject skeds() { return get("skeds"); }

    public static JsonObject manifest() { return get("supply_drop"); }

    static JsonObject get(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try (var in = RadioData.class.getResourceAsStream("/data/yearofice/radio/" + n + ".json")) {
                if (in == null) throw new IllegalArgumentException("missing radio data " + n);
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
