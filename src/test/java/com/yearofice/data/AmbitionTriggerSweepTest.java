package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * story/ambitions_trigger_sweep: every trigger key an ambition file names has a caller of Ambitions.fire in the main sources,
 * except the keys in {@link #UNWIRED} (M9-INT skip list: their owning system has no event yet). The list may only shrink:
 * a key that gains a caller must be deleted from it.
 */
class AmbitionTriggerSweepTest {
    static final List<String> UNWIRED = List.of(
        "bear.backed_away",
        "body.bmi20_30_days",
        "body.no_frostbite_to_april",
        "build.blueprint_cabin",
        "build.cabin_with_stove",
        "build.cellar_100kg_roots",
        "build.crib_bridge",
        "build.fish_wheel_run",
        "build.no_animal_entry",
        "build.outposts_linked_3",
        "crop.seed_saved",
        "fire.relight_from_coals",
        "fire.solstice_kept",
        "food.bread_from_own_wheat",
        "food.cache_30_days",
        "food.dried_fish_10kg",
        "food.last_cracker_and_catch",
        "food.own_cabbage_in_march",
        "hunt.caribou_crossing",
        "hunt.clean_10",
        "hunt.grizzly_bow",
        "livestock.winter_survived",
        "map.regions_40pct",
        "ore.surface_iron",
        "plants.identified_20",
        "sleep.july_no_mosquito",
        "spirits.steady_dec_jan",
        "trap.line_20",
        "travel.canoe_1km",
        "travel.landmark_from_door",
        "travel.long_falls_portage",
        "travel.windgap_in_january",
        "wolf.drove_off_no_loss",
        "wood.seasoned_300");

    @Test
    void everyTriggerHasACaller() throws Exception {
        String src = sources();
        TreeSet<String> missing = new TreeSet<>(), stale = new TreeSet<>();
        try (Stream<Path> s = Files.list(Path.of("src/main/resources/data/yearofice/ambition"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json") && !f.endsWith("groups.json"))::iterator) {
                var o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                for (String k : new String[] {"opens_on", "closes_on"}) {
                    String t = o.get(k).getAsString();
                    if (t.isEmpty()) continue;
                    boolean called = src.contains("\"" + t + "\"");
                    if (!called && !UNWIRED.contains(t)) missing.add(t);
                    if (called && UNWIRED.contains(t)) stale.add(t);
                }
            }
        }
        assertTrue(missing.isEmpty(), "triggers with no caller: " + missing);
        assertTrue(stale.isEmpty(), "wired now, remove from UNWIRED: " + stale);
    }

    private static String sources() throws Exception {
        StringBuilder b = new StringBuilder();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java") && !f.endsWith("RadioGameTests.java"))::iterator) b.append(Files.readString(p));
        }
        return b.toString();
    }
}
