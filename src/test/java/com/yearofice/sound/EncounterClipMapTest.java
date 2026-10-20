package com.yearofice.sound;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncounterClipMapTest {
    @Test
    void everySpeciesBeatMapsToAShippedClip() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/yearofice/animations/hands.animation.json")) {
            var clips = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject().getAsJsonObject("animations").keySet();
            for (String sp : List.of("grizzly", "black_bear", "moose", "wolf", "coyote"))
                for (String beat : List.of("warn", "bluff_charge", "contact", "retreat")) {
                    String c = EncounterBridge.clipForOutcome(sp, beat);
                    assertTrue(c.isEmpty() || clips.contains(c), sp + " " + beat + " -> " + c);
                }
            assertEquals("cover_neck", EncounterBridge.clipForOutcome("grizzly", "contact"));
            assertEquals("", EncounterBridge.clipForOutcome("hare", "contact"));
        }
    }
}
