package com.yearofice.sim.plant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Where each plant grows (Plants and fungi › where each grows): the {@code habitat} tags of the plant files, mapped
 * onto the cover classes of the valley (Stage 6) plus slope, aspect and the start site's disturbed ground. Pure, no
 * Minecraft imports, so JUnit proves that no habitat tag is left unplaced.
 */
public final class PlantHabitat {
    private PlantHabitat() {}

    /** What a habitat reads about a place: its cover class, slope, aspect and the distance to the start site. */
    public record Site(String cls, double slopeDeg, boolean south, boolean north, double startDistM) {}

    /** One species as the placement pass needs it. */
    public record Entry(String id, List<String> habitat, int clumpsLo, int clumpsHi) {}

    static final Set<String> FOREST = Set.of("spruce_taiga", "spruce_moss", "black_spruce_flats", "birch_slope", "lichen_woodland", "foothill_benches", "willow_flat");
    static final Set<String> TAIGA = Set.of("spruce_taiga", "spruce_moss", "foothill_benches", "lichen_woodland");
    static final Set<String> MEADOW = Set.of("foothill_benches", "willow_flat", "birch_slope");
    static final Set<String> RIVER = Set.of("bar_edge", "willow_flat", "cut_bank", "gravel_bar");
    static final Set<String> BOG = Set.of("muskeg", "black_spruce_flats");
    static final Set<String> GRAVEL = Set.of("gravel_bar", "bar_edge");
    static final Set<String> ALPINE = Set.of("alpine_tundra", "scree");
    static final Set<String> ROCK = Set.of("scree", "alpine_tundra");
    public static final Set<String> NOT_PLACED = Set.of("open_river", "lake", "rock_face");

    /** Whether the plant's habitat tag holds at a site. Unknown tags never fit ({@link #known} says which are known). */
    public static boolean fits(String tag, Site s) {
        String c = s.cls();
        if (NOT_PLACED.contains(c)) return false;
        return switch (tag) {
            case "spruce_taiga_open", "burn", "burn_edge" -> TAIGA.contains(c);
            case "meadow", "moist_meadow", "dry_meadow" -> MEADOW.contains(c);
            case "wet_meadow", "beaver_meadow" -> c.equals("willow_flat") || c.equals("muskeg");
            case "riverbank", "wet_riverbank", "river_bar", "creek_bottom", "slough" -> RIVER.contains(c);
            case "south_bluff", "south_slope", "dry_slope" -> (c.equals("birch_slope") || c.equals("foothill_benches")) && s.south();
            case "birch_wood", "birch_slope", "birch_bark", "aspen_wood" -> c.equals("birch_slope");
            case "bog", "bog_edge", "fen", "muskeg_edge", "cloudberry_muskeg" -> BOG.contains(c);
            case "pond_edge", "pond", "lake_edge", "open_water_edge" -> c.equals("willow_flat") || c.equals("muskeg") || c.equals("bar_edge");
            case "gravel_terrace", "gravel", "gravel_bar", "braided_flats", "terrace" -> GRAVEL.contains(c) || c.equals("foothill_benches");
            case "tundra", "alpine_heath", "alpine_meadow", "wet_subalpine_meadow", "alpine_seep" -> c.equals("alpine_tundra");
            case "subalpine" -> c.equals("lichen_woodland") || c.equals("alpine_tundra");
            case "alpine_slope", "alpine_ridge", "alpine_ledge", "cliff", "boulder", "rocky_slope" -> ROCK.contains(c);
            case "wet_slope", "moist_forest", "mossy_forest_floor", "old_spruce" -> c.equals("spruce_moss") || c.equals("spruce_taiga") || c.equals("black_spruce_flats");
            case "disturbed_soil", "trail_edge", "worn_ground", "old_garden" -> s.startDistM() < 300 && FOREST.contains(c);
            default -> false;
        };
    }

    /** Whether {@link #fits} knows the tag at all. */
    public static boolean known(String tag) {
        for (String c : List.of("spruce_taiga", "alpine_tundra", "willow_flat", "birch_slope", "muskeg", "gravel_bar", "scree", "foothill_benches",
                "lichen_woodland", "spruce_moss", "black_spruce_flats", "bar_edge")) {
            if (fits(tag, new Site(c, 10, true, false, 0))) return true;
        }
        return false;
    }

    /** All plant files, read from the classpath in catalogue order. */
    public static List<Entry> load(String[] species) {
        List<Entry> out = new ArrayList<>();
        for (String id : species) {
            try (InputStream in = PlantHabitat.class.getResourceAsStream("/data/yearofice/plant/" + id + ".json")) {
                if (in == null) continue;
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<String> hab = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("habitat")) hab.add(e.getAsString());
                JsonObject patch = o.getAsJsonObject("patch");
                out.add(new Entry("yearofice:" + id, List.copyOf(hab), patch.getAsJsonArray("clumps").get(0).getAsInt(),
                        patch.getAsJsonArray("clumps").get(1).getAsInt()));
            } catch (IOException ex) {
                throw new IllegalStateException(id, ex);
            }
        }
        return List.copyOf(out);
    }

    public static boolean fitsAny(Entry e, Site s) {
        for (String t : e.habitat()) if (fits(t, s)) return true;
        return false;
    }
}
