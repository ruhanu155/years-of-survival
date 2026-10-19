package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The registry check against {@code studio/inventory.json} (Content pipeline › The content linter, check 13, and the
 * registry-dump table): an id the mod registers but the inventory does not plan fails the build as "unplanned id",
 * and a retired id must never be registered. Few ids are registered at M0, so this mostly guards the future.
 */
class InventoryTest {
    static final Path ROOT = Path.of("").toAbsolutePath();
    static JsonObject inventory;
    /** Inventory id (without namespace) to its entries' types and statuses. */
    static final Map<String, List<JsonObject>> BY_ID = new HashMap<>();

    @BeforeAll
    static void load() throws IOException {
        inventory = JsonParser.parseString(Files.readString(ROOT.resolve("studio/inventory.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement el : inventory.getAsJsonArray("entries")) {
            JsonObject e = el.getAsJsonObject();
            String id = e.get("id").getAsString();
            assertTrue(id.startsWith("yearofice:"), id + " lacks the yearofice namespace");
            BY_ID.computeIfAbsent(id.substring("yearofice:".length()), k -> new ArrayList<>()).add(e);
        }
    }

    @Test
    void everyRegisteredIdIsPlanned() {
        List<String> problems = new ArrayList<>();
        for (RegisteredIds.Found f : RegisteredIds.scan(ROOT)) problems.addAll(check(f));
        assertTrue(problems.isEmpty(), "registered ids missing from studio/inventory.json (add them in the same pull request):\n  "
            + String.join("\n  ", problems));
    }

    @Test
    void entriesAreWellFormed() throws IOException {
        JsonObject own = JsonParser.parseString(Files.readString(ROOT.resolve("studio/ownership.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> departments = own.getAsJsonObject("departments").keySet();
        Set<String> seen = new HashSet<>();
        int n = 0;
        for (JsonElement el : inventory.getAsJsonArray("entries")) {
            JsonObject e = el.getAsJsonObject();
            String key = e.get("type").getAsString() + " " + e.get("id").getAsString();
            assertTrue(seen.add(key), "duplicate entry " + key);
            assertTrue(departments.contains(e.get("owner").getAsString()), key + ": owner is not a department in studio/ownership.json");
            boolean retired = e.get("status").getAsString().equals("retired");
            assertTrue(retired == e.get("milestone").isJsonNull(), key + ": only retired ids have no milestone");
            if (!retired) assertTrue(e.get("milestone").getAsString().matches("m\\d+(_5)?"), key + ": bad milestone");
            n++;
        }
        assertEquals(inventory.getAsJsonObject("counts").get("total").getAsInt(), n, "counts.total");
    }

    /** The scanner itself: a planned id passes, an unplanned or retired one is reported. */
    @Test
    void scannerFindsRegistrations() {
        String src = """
            import net.neoforged.neoforge.registries.DeferredRegister;
            class Blocks {
                static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("yearofice");
                static final Object DEEP = BLOCKS.registerSimpleBlock("deep_ground", null);
                static final Object ODD = BLOCKS.register("not_a_planned_block", () -> null);
                static final Object FEVER = EFFECTS.register("fever", () -> null);
            }
            """;
        Set<String> found = RegisteredIds.scanJava(src, "fixture").stream().map(RegisteredIds.Found::id).collect(Collectors.toSet());
        assertEquals(Set.of("deep_ground", "not_a_planned_block", "fever"), found);
        assertTrue(check(new RegisteredIds.Found("deep_ground", null, "fixture")).isEmpty());
        assertEquals(1, check(new RegisteredIds.Found("not_a_planned_block", null, "fixture")).size());
        assertEquals(1, check(new RegisteredIds.Found("fever", null, "fixture")).size(), "fever is a retired mob effect");
        assertEquals(1, check(new RegisteredIds.Found("deep_ground", "tag", "fixture")).size(), "deep_ground is a block, not a tag");

        String layers = """
            void onLayers(RegisterGuiLayersEvent e) {
                e.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "vitals_strip"), null);
            }
            """;
        assertEquals(Set.of("vitals_strip"), RegisteredIds.scanJava(layers, "fixture").stream().map(RegisteredIds.Found::id).collect(Collectors.toSet()));
        assertTrue(RegisteredIds.scanJava("class Commands { void x() { d.register(Commands.literal(\"yoi\")); } }", "fixture").isEmpty());
    }

    private static List<String> check(RegisteredIds.Found f) {
        List<JsonObject> entries = BY_ID.getOrDefault(f.id(), List.of());
        List<JsonObject> typed = f.type() == null ? entries : entries.stream().filter(e -> e.get("type").getAsString().equals(f.type())).toList();
        if (typed.isEmpty()) return List.of("unplanned id yearofice:" + f.id() + (f.type() == null ? "" : " (" + f.type() + ")") + " in " + f.where());
        if (typed.stream().allMatch(e -> e.get("status").getAsString().equals("retired")))
            return List.of("retired id yearofice:" + f.id() + " is registered in " + f.where());
        return List.of();
    }
}
