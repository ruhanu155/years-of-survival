package com.yearofice.ecosystem.livestock;

import com.google.gson.JsonParser;
import com.yearofice.sim.livestock.Spec;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The three livestock specs, read once from {@code data/yearofice/livestock/<id>.json} on the classpath. */
public final class LivestockSpecs {
    private LivestockSpecs() {}

    private static final Map<String, Spec> CACHE = new ConcurrentHashMap<>();

    public static Spec of(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (var in = LivestockSpecs.class.getResourceAsStream("/data/yearofice/livestock/" + p + ".json")) {
                if (in == null) throw new IllegalStateException("no livestock spec " + p);
                return Spec.parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
