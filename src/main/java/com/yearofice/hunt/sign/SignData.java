package com.yearofice.hunt.sign;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One species' sign habits (data {@code sign/<species>.json}): the stride between prints and which sign it leaves. */
public record SignData(String species, double trackEveryM, double printCm, boolean scat, boolean browse, boolean bed) {
    private static final Map<String, SignData> CACHE = new ConcurrentHashMap<>();
    private static final SignData NONE = new SignData("", 0, 0, false, false, false);

    /** The file for a species id (with or without the namespace), or null for a species that leaves none. */
    public static SignData of(String speciesId) {
        String id = speciesId.substring(speciesId.indexOf(':') + 1);
        SignData d = CACHE.computeIfAbsent(id, SignData::load);
        return d == NONE ? null : d;
    }

    private static SignData load(String id) {
        try (var in = SignData.class.getResourceAsStream("/data/yearofice/sign/" + id + ".json")) {
            if (in == null) return NONE;
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new SignData(id, o.get("track_every_m").getAsDouble(), o.get("print_cm").getAsDouble(), o.get("scat").getAsBoolean(),
                    o.get("browse").getAsBoolean(), o.get("bed").getAsBoolean());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(id, e);
        }
    }
}
