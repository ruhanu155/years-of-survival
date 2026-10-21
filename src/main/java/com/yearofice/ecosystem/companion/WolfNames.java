package com.yearofice.ecosystem.companion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Names for a wolf that reaches Tolerant (Taming a wolf: naming becomes possible at Tolerant), and the legend slots
 * (the Black Wolf shares the lone-disperser slot) from {@code data/yearofice/companion/names.json}.
 */
public final class WolfNames {
    private WolfNames() {}

    private static List<String> names;
    private static double blackWolfPct;

    private static synchronized void load() {
        if (names != null) return;
        names = new ArrayList<>();
        try (var in = WolfNames.class.getResourceAsStream("/data/yearofice/companion/names.json")) {
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            o.getAsJsonArray("names").forEach(e -> names.add(e.getAsString()));
            for (var e : o.getAsJsonArray("legends")) blackWolfPct = e.getAsJsonObject().get("chance_pct").getAsDouble();
        } catch (Exception e) {
            names.add("Wolf");
        }
    }

    public static String pick(RandomSource r) {
        load();
        return names.get(r.nextInt(names.size()));
    }

    /** Percent of lone dispersers that are the Black Wolf legend. */
    public static double blackWolfPct() {
        load();
        return blackWolfPct;
    }
}
