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
    static final List<String> UNWIRED = List.of();

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
