package com.yearofice.hunt.fishing;

import com.google.gson.JsonParser;
import com.yearofice.sim.fishing.FishData;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishSpecies;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The fish species, read from {@code data/yearofice/fish/<id>.json} (Fishing in depth › Species data). Both sides of
 * the lockstep fight resolve a species by id here, so the files are read from the mod's own resources (the
 * bootstrap reader) rather than a server reload listener the client never sees.
 */
public final class FishingSpecies {
    private FishingSpecies() {}

    public static final String GRAYLING = "arctic_grayling";

    /** The ids that have a file and a landed-fish item. */
    public static final List<String> IDS = List.of("arctic_grayling", "dolly_varden", "northern_pike", "humpback_whitefish",
            "burbot", "lake_trout", "chinook_salmon", "chum_salmon", "coho_salmon");

    /** Worldgen's short spot-species names to species ids. */
    private static final Map<String, String> ALIAS = Map.of("grayling", "arctic_grayling", "pike", "northern_pike",
            "whitefish", "humpback_whitefish");

    private static final Map<String, FishData> DATA = new TreeMap<>();

    public static synchronized FishData data(String id) {
        FishData d = DATA.get(id);
        if (d != null) return d;
        try (Reader r = new InputStreamReader(FishingSpecies.class.getResourceAsStream("/data/yearofice/fish/" + id + ".json"), StandardCharsets.UTF_8)) {
            d = FishData.parse(JsonParser.parseReader(r).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("unknown or bad fish species " + id, e);
        }
        DATA.put(id, d);
        return d;
    }

    public static boolean has(String id) { return IDS.contains(id); }

    public static String canon(String name) { return ALIAS.getOrDefault(name, name); }

    public static FishSpecies byId(String id) { return data(id).species(); }

    public static FishSeason season(String id) { return data(id).season(); }

    public static FishSpecies grayling() { return byId(GRAYLING); }
}
