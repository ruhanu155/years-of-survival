package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** RECONCILE 301: every item registered through an ITEMS register call has models/item/<id>.json (no "Unable to load model"). */
class ItemModelCoverageTest {
    private static final Pattern P = Pattern.compile("\\bITEMS\\.register(?:Simple)?(?:Block)?Item\\(\\s*\"([a-z0-9_]+)\"");

    @Test
    void everyRegisteredItemHasAModel() throws Exception {
        Path root = Path.of("src/main");
        TreeSet<String> missing = new TreeSet<>();
        try (Stream<Path> s = Files.walk(root.resolve("java"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = P.matcher(Files.readString(p));
                while (m.find()) {
                    if (!Files.exists(root.resolve("resources/assets/yearofice/models/item/" + m.group(1) + ".json"))) missing.add(m.group(1));
                }
            }
        }
        assertTrue(missing.isEmpty(), "items without a model: " + missing);
    }
}
