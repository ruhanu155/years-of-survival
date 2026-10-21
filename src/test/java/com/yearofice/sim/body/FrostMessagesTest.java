package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Every frost stage transition of every zone (up and down) has a Messenger line in the lang file. */
class FrostMessagesTest {

    @Test
    void everyZoneStageTransitionHasAMessageKey() throws IOException {
        JsonObject lang;
        try (InputStream in = getClass().getResourceAsStream("/assets/yearofice/lang/en_us.json")) {
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> keys = new HashSet<>();
        int transitions = 0;
        for (Zone z : Zone.values()) {
            for (int before = 0; before <= 3; before++) {
                for (int after = 0; after <= 3; after++) {
                    String k = FrostStep.eventKey(z, before, after);
                    if (before == after) { assertNull(k); continue; }
                    transitions++;
                    assertNotNull(k);
                    keys.add(k);
                    String text = lang.has("messenger.yearofice.body." + k) ? lang.get("messenger.yearofice.body." + k).getAsString() : null;
                    assertTrue(text != null && !text.isBlank(), "no lang line for " + z + " " + before + "->" + after + " (" + k + ")");
                }
            }
        }
        assertEquals(60, transitions);
        assertEquals(25, keys.size(), "5 zones x (nip, superficial, deep, clear, ease)");
    }
}
