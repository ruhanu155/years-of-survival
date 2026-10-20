package com.yearofice.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.TestFunction;

/**
 * Local GameTest filter: {@code ./gradlew runGameTestServer -Pgametest.filter=hud/,fire/} keeps only tests whose id
 * (after {@code yearofice:}) starts with one of the comma-separated prefixes. Unset means everything; CI never sets it.
 */
public final class TestFilter {
    private TestFilter() {}

    private static final List<String> PREFIXES = parse(System.getProperty("yearofice.gametest.filter", ""));

    static List<String> parse(String s) {
        List<String> out = new ArrayList<>();
        for (String p : s.split(",")) {
            String t = p.trim();
            if (t.startsWith("yearofice:")) t = t.substring("yearofice:".length());
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public static boolean keep(String id) { return keep(id, PREFIXES); }

    static boolean keep(String id, List<String> prefixes) {
        if (prefixes.isEmpty()) return true;
        String bare = id.startsWith("yearofice:") ? id.substring("yearofice:".length()) : id;
        for (String p : prefixes) if (bare.startsWith(p)) return true;
        return false;
    }

    /** Call as {@code return TestFilter.apply(out);} at the end of a {@code @GameTestGenerator}. */
    public static List<TestFunction> apply(Collection<TestFunction> tests) {
        List<TestFunction> out = new ArrayList<>();
        for (TestFunction t : tests) if (keep(t.testName())) out.add(t);
        return out;
    }
}
