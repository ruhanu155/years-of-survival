package com.yearofice.sim.story;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.yearofice.sim.story.SkyLore.*;
import java.io.FileReader;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SkyLoreTest {
    @Test
    void signsAreHonestOddsAndSkillLimitsWhatIsNoticed() throws Exception {
        var table = SkyLore.signs(JsonParser.parseReader(new FileReader("src/main/resources/data/yearofice/story/sky_signs.json")).getAsJsonObject());
        assertTrue(table.containsKey("aurora") && table.containsKey("sun_dogs"));
        table.values().forEach(s -> assertFalse(s.lore().isBlank()));
        var cold = new Tomorrow(-30, -28, 0, 1); // colder, still, dry: sun dogs hold only if not warmer
        var rng = new Random(7);
        int n = 20000, dogs = 0, most = 0;
        for (int i = 0; i < n; i++) {
            var shown = SkyLore.shown(table, cold, rng);
            if (shown.stream().anyMatch(s -> s.id().equals("sun_dogs"))) dogs++;
            if (SkyLore.noticed(shown, Level.CLUMSY).size() > 1 || SkyLore.noticed(shown, Level.HANDY).size() > 3) fail();
            most = Math.max(most, SkyLore.noticed(shown, Level.PRACTICED).size());
        }
        assertEquals(0.8, dogs / (double) n, 0.02);
        assertEquals(table.size(), most, "all five can show together");
        assertEquals(0.55, SkyLore.right(Level.CLUMSY));
    }

    @Test
    void mistNeedsCalmOpenWarmWaterAndADryNight() {
        assertTrue(MistRule.mist(true, 1, 8, 2, false));
        assertFalse(MistRule.mist(true, 3, 8, 2, false));
        assertFalse(MistRule.mist(false, 1, 8, 2, false));
        assertFalse(MistRule.mist(true, 1, 4, 2, false));
        assertFalse(MistRule.mist(true, 1, 8, 2, true));
    }
}
