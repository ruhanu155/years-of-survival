package com.yearofice.client.input;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The 13 key mappings of Input and keybinds, as pure data (GLFW key codes), so a JUnit test can hold them against
 * vanilla's defaults without starting Minecraft. Minecraft's defaults win; the three vanilla keys our defaults take
 * (L advancements, X and C the creative hotbar) are unbound on first launch.
 */
public final class KeyTable {
    private KeyTable() {}

    public static final int R = 82, M = 77, Z = 90, G = 71, V = 86, B = 66, L = 76, X = 88, C = 67, O = 79, N = 78, K = 75,
            LEFT_ALT = 342;

    /** Mapping id (after {@code key.yearofice.}) to its default key, in the doc's order. */
    public static final Map<String, Integer> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("quick_eat", R);
        DEFAULTS.put("messenger", M);
        DEFAULTS.put("rest", Z);
        DEFAULTS.put("whistle", G);
        DEFAULTS.put("voice", V);
        DEFAULTS.put("spray", B);
        DEFAULTS.put("light", L);
        DEFAULTS.put("read", X);
        DEFAULTS.put("binoculars", C);
        DEFAULTS.put("open_coat", O);
        DEFAULTS.put("mittens", N);
        DEFAULTS.put("rotate_modifier", LEFT_ALT);
        DEFAULTS.put("blueprint", K);
    }

    /** Vanilla's mappings unbound on first launch: advancements (L), save and load hotbar (C, X). */
    public static final Set<String> UNBIND_ON_FIRST_LAUNCH = Set.of("key.advancements", "key.saveToolbarActivator", "key.loadToolbarActivator");
}
