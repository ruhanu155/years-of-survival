package com.yearofice.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Prototype C1, the part a JUnit can hold: the registered order is the doc's 15-row table, bottom to top. */
class HudStackTest {
    static List<String> docOrder(String heading) throws Exception {
        List<String> lines = Files.readAllLines(Path.of("docs/blueprint/04-minecraft-development.md"), StandardCharsets.UTF_8);
        int i = lines.indexOf(heading);
        assertTrue(i >= 0, "heading " + heading);
        Pattern row = Pattern.compile("^\\| (\\d+) \\| `yearofice:([a-z_]+)` \\|");
        List<String> out = new ArrayList<>();
        for (int j = i + 1; j < lines.size() && !lines.get(j).startsWith("### "); j++) {
            Matcher m = row.matcher(lines.get(j));
            if (m.find()) {
                assertEquals(out.size() + 1, Integer.parseInt(m.group(1)), "row numbers run 1..15");
                out.add(m.group(2));
            }
        }
        return out;
    }

    @Test
    void orderMatchesBothDocTables() throws Exception {
        assertEquals(16, HudStack.ORDER.size());
        List<String> layer8 = docOrder("### Layer 8: Messenger and HUD bus");
        assertEquals(HudStack.ORDER, layer8, "Architecture › Layer 8 (the 16th, field_note_card, ships in M3)");
        assertEquals(HudStack.ORDER.subList(0, 15), docOrder("### The HUD layer stack"), "Player systems and client › The HUD layer stack lists the first 15");
        assertEquals(List.of("headwear", "frost", "body_fx"), HudStack.ORDER.subList(0, HudStack.VIEW_LAYERS), "F1 leaves layers 1-3");
        assertEquals(List.of(5, 6, 10), HudStack.BATTERY_LAYERS.stream().map(HudStack::number).toList(), "battery 0 blanks 5, 6 and 10");
    }
}
