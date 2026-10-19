package com.yearofice.client.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@code KeyMapTest.no_default_conflicts} (GameTests and client tests): no default of ours meets a vanilla default after the first-launch unbinds. */
class KeyMapTest {
    /** Vanilla 1.21.1's default keyboard bindings, by mapping name. */
    static final Map<String, Integer> VANILLA = Map.ofEntries(
            Map.entry("key.forward", 87), Map.entry("key.left", 65), Map.entry("key.back", 83), Map.entry("key.right", 68),
            Map.entry("key.jump", 32), Map.entry("key.sneak", 340), Map.entry("key.sprint", 341), Map.entry("key.inventory", 69),
            Map.entry("key.swapOffhand", 70), Map.entry("key.drop", 81), Map.entry("key.chat", 84), Map.entry("key.playerlist", 258),
            Map.entry("key.command", 47), Map.entry("key.socialInteractions", 80), Map.entry("key.screenshot", 291),
            Map.entry("key.togglePerspective", 294), Map.entry("key.fullscreen", 300), Map.entry("key.advancements", 76),
            Map.entry("key.saveToolbarActivator", 67), Map.entry("key.loadToolbarActivator", 88),
            Map.entry("key.hotbar.1", 49), Map.entry("key.hotbar.2", 50), Map.entry("key.hotbar.3", 51), Map.entry("key.hotbar.4", 52),
            Map.entry("key.hotbar.5", 53), Map.entry("key.hotbar.6", 54), Map.entry("key.hotbar.7", 55), Map.entry("key.hotbar.8", 56),
            Map.entry("key.hotbar.9", 57));

    @Test
    void noDefaultConflicts() {
        assertEquals(13, KeyTable.DEFAULTS.size(), "13 key mappings (Input and keybinds)");
        Set<Integer> vanilla = new HashSet<>();
        VANILLA.forEach((name, code) -> { if (!KeyTable.UNBIND_ON_FIRST_LAUNCH.contains(name)) vanilla.add(code); });
        Set<Integer> ours = new HashSet<>();
        KeyTable.DEFAULTS.forEach((id, code) -> {
            assertTrue(ours.add(code), id + " shares a default with another of ours");
            assertTrue(!vanilla.contains(code), id + " meets a vanilla default");
        });
        assertEquals(KeyTable.R, KeyTable.DEFAULTS.get("quick_eat"));
        assertEquals(KeyTable.X, KeyTable.DEFAULTS.get("read"));
        assertEquals(KeyTable.G, KeyTable.DEFAULTS.get("whistle"));
    }
}
