package com.yearofice.sim.story;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import org.junit.jupiter.api.Test;

class RareEventsTest {
    @Test
    void everyEventLandsWithinFivePointsOfItsChanceOverAHundredYears() throws Exception {
        File[] files = new File("src/main/resources/data/yearofice/event").listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files);
        int checked = 0;
        for (File f : files) {
            JsonObject o;
            try (FileReader r = new FileReader(f)) { o = JsonParser.parseReader(r).getAsJsonObject(); }
            var d = new RareEvents.Def(o.get("id").getAsString(), o.get("chance_per_year").getAsDouble(),
                    o.has("from_year") ? o.get("from_year").getAsInt() : 1);
            int hits = 0, years = 0;
            for (int seed = 0; seed < 40; seed++) // 1000 world-years: the 5-point band is ~3 sigma at this size
                for (int y = 2; y < 27; y++) { years++; if (RareEvents.rolls(d, y, seed)) hits++; }
            assertEquals(d.chancePerYear(), hits / (double) years, 0.05, d.id());
            checked++;
        }
        assertEquals(8, checked);
    }

    @Test
    void firstYearSkipsTheLateEvents() {
        for (int s = 0; s < 50; s++) assertFalse(RareEvents.rolls(new RareEvents.Def("x", 1.0, 2), 1, s));
    }
}
