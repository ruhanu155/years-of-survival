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
        assertEquals(15, HudStack.ORDER.size());
        assertEquals(docOrder("### The HUD layer stack"), HudStack.ORDER, "Player systems and client › The HUD layer stack");
        // Layer 8's table also lists the 16th layer, field_note_card, which ships in M3 (video 2, A8).
        List<String> layer8 = docOrder("### Layer 8: Messenger and HUD bus");
        assertEquals(HudStack.ORDER, layer8.subList(0, 15), "Architecture › Layer 8");
        assertEquals(List.of("field_note_card"), layer8.subList(15, layer8.size()), "the M3 layer after the 15");
        assertEquals(List.of("headwear", "frost", "body_fx"), HudStack.ORDER.subList(0, HudStack.VIEW_LAYERS), "F1 leaves layers 1-3");
        assertEquals(List.of(5, 6, 10), HudStack.BATTERY_LAYERS.stream().map(HudStack::number).toList(), "battery 0 blanks 5, 6 and 10");
    }
}
