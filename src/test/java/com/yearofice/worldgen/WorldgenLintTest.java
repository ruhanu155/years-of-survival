package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The worldgen lint (Minecraft engine › Determinism; {@code tools/lint-gen} in the blueprint), as a
 * source-scanning test. Over {@code com.yearofice.worldgen} and {@code com.yearofice.sim.worldgen}, after
 * comments and string literals are stripped, it fails on:
 * <ul>
 *   <li>{@code Math.random}, {@code java.util.Random}, {@code ThreadLocalRandom}, {@code SplittableRandom},
 *       {@code SecureRandom};</li>
 *   <li>wall-clock reads: {@code System.currentTimeMillis}, {@code System.nanoTime}, {@code Instant.now},
 *       {@code java.time *.now}, {@code Clock.system*}, {@code new Date(}, {@code Calendar.getInstance};</li>
 *   <li>inexact {@code Math} functions ({@code sin, cos, tan, exp, log, pow, ...}); {@code StrictMath} is the
 *       allowed route where trig is unavoidable;</li>
 *   <li>{@code HashMap}/{@code HashSet} and parallel streams in the pure stages, so no result can depend on
 *       iteration or scheduling order;</li>
 *   <li>Minecraft, Mojang or NeoForge imports in the pure stages. Adapters may import them: anything under
 *       {@code worldgen/debug/}, {@code worldgen/structure/}, {@code worldgen/gen/} (the chunk generator, biome
 *       source and features) and {@code worldgen/block/}.</li>
 * </ul>
 */
class WorldgenLintTest {
    static final Path ROOT = Paths.get("src/main/java/com/yearofice");
    static final List<Path> SCANNED = List.of(ROOT.resolve("worldgen"), ROOT.resolve("sim/worldgen"));
    static final List<String> ADAPTER_FILES = List.of("YoiChunkGenerator.java", "YoiBiomeSource.java");

    record Rule(String name, Pattern pattern, boolean pureOnly) {}

    static final List<Rule> RULES = List.of(
            new Rule("Math.random", Pattern.compile("\\bMath\\s*\\.\\s*random\\b"), false),
            new Rule("java.util.Random", Pattern.compile("\\b(java\\.util\\.)?(ThreadLocal|Splittable|Secure)?Random\\b"), false),
            new Rule("wall clock", Pattern.compile("\\bSystem\\s*\\.\\s*(currentTimeMillis|nanoTime)\\b"
                    + "|\\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|Year|YearMonth)\\s*\\.\\s*now\\b"
                    + "|\\bClock\\s*\\.\\s*system|\\bnew\\s+(java\\.util\\.)?Date\\s*\\(|\\bCalendar\\s*\\.\\s*getInstance\\b"), false),
            new Rule("inexact Math function", Pattern.compile(
                    "\\bMath\\s*\\.\\s*(sin|cos|tan|asin|acos|atan|atan2|sinh|cosh|tanh|exp|expm1|log|log10|log1p|pow|cbrt|hypot)\\b"), false),
            new Rule("HashMap/HashSet", Pattern.compile("\\b(HashMap|HashSet)\\b"), true),
            new Rule("parallel stream", Pattern.compile("\\bparallelStream\\b|\\.\\s*parallel\\s*\\("), true),
            new Rule("Minecraft import", Pattern.compile("\\bimport\\s+(static\\s+)?(net\\.minecraft|com\\.mojang|net\\.neoforged)\\."), true));

    @Test
    void worldgenSourcesPassTheLint() throws IOException {
        List<String> violations = new ArrayList<>();
        int files = 0;
        for (Path dir : SCANNED) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path f : s.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                    files++;
                    violations.addAll(scan(f.toString().replace('\\', '/'), Files.readString(f, StandardCharsets.UTF_8)));
                }
            }
        }
        assertTrue(files >= 8, "lint found only " + files + " worldgen sources; is the working directory the project?");
        assertTrue(violations.isEmpty(), "worldgen lint:\n" + String.join("\n", violations));
    }

    @Test
    void lintCatchesEachBannedCall() {
        String bad = String.join("\n",
                "package com.yearofice.worldgen;",
                "import net.minecraft.world.level.Level;",
                "import java.util.Random;",
                "class Bad {",
                "  double a = Math.random();",
                "  long t = System.nanoTime() + System.currentTimeMillis();",
                "  double s = Math.sin(1) + Math.pow(2, 3);",
                "  Object m = new java.util.HashMap<String, String>();",
                "  Object n = java.time.Instant.now();",
                "  Object r = java.util.concurrent.ThreadLocalRandom.current();",
                "}");
        List<String> v = scan("src/main/java/com/yearofice/worldgen/Bad.java", bad);
        for (String rule : List.of("Math.random", "java.util.Random", "wall clock", "inexact Math function",
                "HashMap/HashSet", "Minecraft import")) {
            assertTrue(v.stream().anyMatch(s -> s.contains(rule)), "lint missed " + rule + " in " + v);
        }
    }

    @Test
    void lintIgnoresCommentsStringsStrictMathAndAdapters() {
        String ok = String.join("\n",
                "package com.yearofice.worldgen;",
                "/** Never call Math.random() or System.nanoTime() here. */",
                "class Ok {",
                "  // Math.sin is banned; StrictMath is fine",
                "  double t = StrictMath.tan(0.5) + Math.sqrt(2) + Math.floor(1.5);",
                "  String s = \"Math.pow(2, 3) and new Random()\";",
                "  Object m = new java.util.LinkedHashMap<String, String>();",
                "}");
        assertEquals(List.of(), scan("src/main/java/com/yearofice/worldgen/Ok.java", ok));
        String adapter = "package com.yearofice.worldgen.debug;\nimport net.minecraft.commands.Commands;\nclass A {}";
        assertEquals(List.of(), scan("src/main/java/com/yearofice/worldgen/debug/A.java", adapter));
    }

    static boolean isAdapter(String path) {
        if (path.contains("/worldgen/debug/") || path.contains("/worldgen/structure/") || path.contains("/worldgen/gen/")
                || path.contains("/worldgen/block/")) return true;
        for (String f : ADAPTER_FILES) if (path.endsWith("/" + f)) return true;
        return false;
    }

    static List<String> scan(String path, String source) {
        String code = strip(source);
        boolean adapter = isAdapter(path);
        List<String> out = new ArrayList<>();
        String[] lines = code.split("\n", -1);
        for (Rule r : RULES) {
            if (r.pureOnly() && adapter) continue;
            for (int i = 0; i < lines.length; i++) {
                Matcher m = r.pattern().matcher(lines[i]);
                if (m.find()) out.add(path + ":" + (i + 1) + ": " + r.name() + " (" + m.group().trim() + ")");
            }
        }
        return out;
    }

    /** Blanks comments, string, char and text-block literals, keeping newlines so line numbers hold. */
    static String strip(String s) {
        StringBuilder b = new StringBuilder(s.length());
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i < n && !(s.charAt(i) == '*' && i + 1 < n && s.charAt(i + 1) == '/')) {
                    if (s.charAt(i) == '\n') b.append('\n');
                    i++;
                }
                i += 2;
            } else if (s.startsWith("\"\"\"", i)) {
                i += 3;
                while (i < n && !s.startsWith("\"\"\"", i)) {
                    if (s.charAt(i) == '\n') b.append('\n');
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                i += 3;
                b.append("\"\"");
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && s.charAt(i) != c && s.charAt(i) != '\n') {
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                i++;
                b.append(c).append(c);
            } else {
                b.append(c);
                i++;
            }
        }
        return b.toString();
    }
}
