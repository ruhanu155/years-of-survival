package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * GeckoLib loads every file under assets/yearofice/geo/ as a model. Any other file there
 * (provenance sidecars, notes) breaks the resource reload and the client hangs on load.
 */
class GeoFolderTest {
    @Test
    void geoFolderHoldsOnlyGeoModels() throws IOException {
        Path geo = Path.of("src/main/resources/assets/yearofice/geo");
        if (!Files.isDirectory(geo)) return;
        try (Stream<Path> files = Files.walk(geo)) {
            List<Path> bad = files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".geo.json"))
                    .toList();
            assertTrue(bad.isEmpty(), "Non-model files under geo/ break GeckoLib loading: " + bad);
        }
    }
}
