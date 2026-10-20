package com.yearofice.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CropDataTest {
    @Test
    void fiveCropsReadWithTheBlueprintWindows() throws IOException {
        for (String c : new String[] {"potato", "barley", "wheat", "turnip", "cabbage"}) {
            CropSpec s = new CropSpec(JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/yearofice/crop/" + c + ".json"))).getAsJsonObject());
            assertEquals("yearofice:" + c, s.id);
            assertTrue(s.windowFromDoy < s.windowToDoy && s.ripeUnits > s.sproutUnits);
        }
        CropSpec b = new CropSpec(JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/yearofice/crop/barley.json"))).getAsJsonObject());
        assertEquals(0.6, b.baseGermination, 1e-9);
        assertEquals(20, b.seedsPerBlock);
        assertEquals(com.yearofice.sim.time.Calendar.dayOfYear(5, 20), b.windowFromDoy);
    }
}
