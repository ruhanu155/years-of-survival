package com.yearofice.worldgen.regions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 16 home-valley regions and the three rings as data ({@code data/yearofice/region/<id>.json}, World map › Three
 * rings). Lookup is by position: ring 1 is x and z in [-4096, 4095], region cells are 2,048 blocks, A1 north-west.
 * ponytail: the layout is the unmirrored default (the seed's east-west mirror is not applied) and the id list is fixed,
 * read from the jar; upgrade: a reload listener and ValleyWorld's mirror flag.
 */
public final class Regions {
    private Regions() {}

    public record Region(String id, String cell, String name, int ring, int[] x, int[] z, Map<String, Double> biomeMix,
                         List<String> outposts, int[] dropZone, int springLagDays, String landmark) {
        public String path() { return id.substring(id.indexOf(':') + 1); }

        public int centreX() { return (x[0] + x[1] + 1) / 2; }

        public int centreZ() { return (z[0] + z[1] + 1) / 2; }
    }

    /** Ring 1 half-width: x and z from -4096 to +4095. */
    public static final int RING1_HALF = 4096, CELL = 2048;
    public static final List<String> IDS = List.of("sheep_crags", "glacier_cirque", "iron_range", "windgap_pass", "caribou_barrens",
            "lookout_bench", "hot_spring_basin", "burn_hills", "slough_country", "outpost_valley", "salmon_bend", "upriver_canyon",
            "big_lake", "beaver_valley", "cloudberry_muskeg", "discovery_creek");

    private static volatile List<Region> all;
    private static volatile JsonObject border;

    public static List<Region> all() {
        List<Region> a = all;
        if (a == null) {
            List<Region> l = new ArrayList<>();
            for (String id : IDS) l.add(read(json("region/" + id + ".json")));
            all = a = List.copyOf(l);
        }
        return a;
    }

    /** By path ({@code salmon_bend}), full id or cell code ({@code C3}); null when unknown. */
    public static Region byId(String key) {
        String k = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        for (Region r : all()) if (r.path().equals(k) || r.cell().equalsIgnoreCase(k)) return r;
        return null;
    }

    /** 1 inside +-4,096, 2 inside +-32,768, 3 beyond (World map › Three rings). */
    public static int ring(int x, int z) {
        if (x >= -RING1_HALF && x < RING1_HALF && z >= -RING1_HALF && z < RING1_HALF) return 1;
        return x >= -32768 && x < 32768 && z >= -32768 && z < 32768 ? 2 : 3;
    }

    /** The region holding a block column; null outside ring 1. */
    public static Region at(int x, int z) {
        if (ring(x, z) != 1) return null;
        int col = Math.floorDiv(x + RING1_HALF, CELL), row = Math.floorDiv(z + RING1_HALF, CELL);
        return all().get(row * 4 + col);
    }

    /** True when an outpost kind may stand at the column: outside ring 1 anything goes, inside the region must list it. */
    public static boolean allowsOutpost(int x, int z, String outpostId) {
        Region r = at(x, z);
        return r == null || r.outposts().contains(outpostId.contains(":") ? outpostId : "yearofice:" + outpostId);
    }

    /** The development border's width for a ring, from {@code region/dev_border.json}. */
    public static double borderSize(int ring) {
        return border().getAsJsonObject("size_m_by_ring").get(String.valueOf(Math.max(1, Math.min(3, ring)))).getAsDouble();
    }

    public static int defaultRing() { return border().get("default_ring").getAsInt(); }

    private static JsonObject border() {
        JsonObject b = border;
        if (b == null) border = b = json("region/dev_border.json");
        return b;
    }

    static Region read(JsonObject o) {
        Map<String, Double> mix = new LinkedHashMap<>();
        o.getAsJsonObject("biome_mix").entrySet().forEach(e -> mix.put(e.getKey(), e.getValue().getAsDouble()));
        JsonObject bd = o.getAsJsonObject("bounds_m");
        List<String> out = new ArrayList<>();
        o.getAsJsonArray("outposts").forEach(e -> out.add(e.getAsString()));
        return new Region(o.get("id").getAsString(), o.get("cell").getAsString(), o.get("name").getAsString(), o.get("ring").getAsInt(),
                pair(bd.getAsJsonArray("x")), pair(bd.getAsJsonArray("z")), mix, out, pair(o.getAsJsonArray("drop_zone_m")),
                o.get("spring_lag_days").getAsInt(), o.get("landmark").getAsString());
    }

    private static int[] pair(JsonArray a) { return new int[] {a.get(0).getAsInt(), a.get(1).getAsInt()}; }

    static JsonObject json(String path) {
        try (var in = Regions.class.getResourceAsStream("/data/yearofice/" + path)) {
            if (in == null) throw new IllegalArgumentException("missing data " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
