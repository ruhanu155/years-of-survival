package com.yearofice.client.hand;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

class HandClipsTest {
    @Test
    void shippedClipsParseAndSample() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/yearofice/animations/hands.animation.json")) {
            var clips = HandClips.parse(JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject());
            assertEquals(java.util.Set.of("cover_neck", "push_up", "desperate_strike", "catch_fall", "stagger"), clips.keySet());
            assertEquals(0.5f, clips.get("desperate_strike").length(), 1e-6);
            assertEquals(1.2f, clips.get("push_up").length(), 1e-6);
            var c = clips.get("cover_neck");
            assertTrue(c.hold());
            assertEquals(c.sample("left_hand", 0.6f).y(), c.sample("left_hand", 5f).y(), 1e-6, "held on the last frame");
            assertTrue(c.sample("left_hand", 0.6f).y() < c.sample("left_hand", 0f).y(), "hands rise to the neck");
        }
    }
}
