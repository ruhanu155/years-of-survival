package com.yearofice.tools.contentlint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * tools/content-lint: the content linter checks that apply today (Minecraft development › The content linter;
 * Art and audio production › The provenance check in CI). Gradle runs it as {@code contentLint}, a dependency of
 * {@code check}.
 *
 * <ol>
 *   <li>{@code keys}: every JSON key under data/yearofice is snake_case, never camelCase (the fix is the doc's
 *       mechanical rule), and spells units as suffixes ({@code _kg}, not {@code _kilograms}).</li>
 *   <li>{@code units}: a number whose key names a measured quantity (weight, depth, time...) carries a unit
 *       suffix, here or on an enclosing key ({@code spoil_days_at_5c.raw}).</li>
 *   <li>{@code names}: data and asset file names are snake_case.</li>
 *   <li>{@code credits}: every asset file has exactly one credits.json entry, every entry's files exist, licences
 *       are on the allowed list, and every found asset names a licence file that exists (and its original, when
 *       committed, matches its hash).</li>
 *   <li>{@code mojang}: the mod jar holds nothing under assets/minecraft/ and no file identical to a vanilla
 *       resource, checked against the Minecraft resources jar when it is present.</li>
 *   <li>{@code codeowners}: .github/CODEOWNERS matches studio/ownership.json.</li>
 * </ol>
 * The inventory is checked for staleness against the doc as a warning only: a new doc id is a ticket, not a
 * broken build.
 *
 * <p>Usage: {@code ContentLint <projectDir> <modJar> [minecraftResourcesJar]}.
 */
public final class ContentLint {
    static final Pattern SNAKE = Pattern.compile("[a-z0-9]+(_[a-z0-9]+)*");
    static final Pattern FILE_NAME = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)*");
    static final Pattern RESOURCE_KEY = Pattern.compile("#?[a-z0-9_.-]+:[a-z0-9_./-]+");
    static final Pattern UNIT_TOKEN = Pattern.compile("\\d*(m|cm|mm|km|m2|m3|km2|kg|g|mg|t|l|ml|c|k|s|ms|min|h|d|days|weeks|years|ticks|steps|cells|blocks|chunks|px|pct|kcal|kj|w|kw|mps|kmh|deg|rad|db|hz)");
    /** Key words that name a measured quantity; such a number needs a unit. */
    static final Set<String> QUANTITY = Set.of("weight", "mass", "length", "height", "depth", "width", "distance", "radius",
        "diameter", "thickness", "elevation", "altitude", "temp", "temperature", "duration", "time", "delay", "interval",
        "age", "speed", "velocity", "area", "volume", "spacing", "size", "energy", "calories", "heat");
    /** Last words that make a key a ratio or a count. */
    static final Set<String> DIMENSIONLESS = Set.of("fraction", "ratio", "factor", "multiplier", "share", "chance",
        "probability", "odds", "count", "index", "boost", "scale");
    /** Unit words spelled out; the doc's suffix replaces each. */
    static final Map<String, String> LONG_UNITS = Map.ofEntries(
        Map.entry("kilograms", "kg"), Map.entry("kilogram", "kg"), Map.entry("kilos", "kg"), Map.entry("grams", "g"),
        Map.entry("gram", "g"), Map.entry("metres", "m"), Map.entry("meters", "m"), Map.entry("metre", "m"),
        Map.entry("meter", "m"), Map.entry("centimetres", "cm"), Map.entry("centimeters", "cm"),
        Map.entry("millimetres", "mm"), Map.entry("millimeters", "mm"), Map.entry("kilometres", "km"),
        Map.entry("kilometers", "km"), Map.entry("celsius", "c"), Map.entry("hours", "h"), Map.entry("hour", "h"),
        Map.entry("hrs", "h"), Map.entry("minutes", "min"), Map.entry("minute", "min"), Map.entry("mins", "min"),
        Map.entry("seconds", "s"), Map.entry("second", "s"), Map.entry("secs", "s"), Map.entry("day", "days"),
        Map.entry("percent", "pct"), Map.entry("percentage", "pct"), Map.entry("litres", "l"), Map.entry("liters", "l"),
        Map.entry("litre", "l"), Map.entry("liter", "l"));
    /** The doc's camelCase fix: a trailing unit word becomes its suffix. */
    static final Map<String, String> CAMEL_UNITS = Map.of("kg", "kg", "hours", "h", "days", "days", "c", "c", "m", "m", "pct", "pct", "g", "g");
    static final Set<String> LICENCES = Set.of("project", "CC0-1.0", "CC-BY-3.0", "CC-BY-4.0", "CC-BY-SA-3.0", "CC-BY-SA-4.0",
        "CC-BY-NC-4.0", "CC-BY-NC-SA-4.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "Zlib", "OFL-1.1", "permission");
    /** Asset files that are not art and carry no credits entry. */
    static final List<String> NOT_ART = List.of("assets/yearofice/credits.json", "assets/yearofice/lang/", "assets/yearofice/sounds.json");

    private final Path root;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Map<String, Integer> checked = new LinkedHashMap<>();

    ContentLint(Path root) { this.root = root; }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        ContentLint lint = new ContentLint(root);
        lint.dataKeys();
        lint.fileNames();
        lint.credits();
        lint.jar(Path.of(args[1]), args.length > 2 ? Path.of(args[2]) : null);
        lint.codeowners();
        lint.inventory();

        StringBuilder report = new StringBuilder();
        lint.checked.forEach((k, v) -> report.append("checked ").append(k).append(": ").append(v).append('\n'));
        lint.warnings.forEach(w -> report.append("warning: ").append(w).append('\n'));
        lint.errors.forEach(e -> report.append("error: ").append(e).append('\n'));
        report.append(lint.errors.isEmpty() ? "CONTENT LINT PASSED" : "CONTENT LINT FAILED: " + lint.errors.size() + " error(s)").append('\n');
        Path out = root.resolve("build/content-lint/report.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        for (String line : report.toString().split("\n")) System.out.println("contentLint: " + line);
        if (!lint.errors.isEmpty()) System.exit(1);
    }

    // ---- 1-2: keys and units -------------------------------------------------------------------------------

    void dataKeys() throws IOException {
        int files = 0;
        for (Path dir : resourceRoots("data/yearofice")) {
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                    files++;
                    String rel = rel(p);
                    JsonElement json;
                    try {
                        json = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
                    } catch (RuntimeException e) {
                        errors.add("[json] " + rel + ": does not parse: " + e.getMessage());
                        continue;
                    }
                    walk(json, rel, "", false);
                }
            }
        }
        checked.put("data JSON files", files);
    }

    private void walk(JsonElement el, String file, String path, boolean unitAbove) {
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) walk(e, file, path, unitAbove);
            return;
        }
        if (!el.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            String k = e.getKey();
            String at = file + " " + (path.isEmpty() ? k : path + "." + k);
            if (RESOURCE_KEY.matcher(k).matches()) { walk(e.getValue(), file, path.isEmpty() ? k : path + "." + k, unitAbove); continue; }
            if (!k.equals(k.toLowerCase(Locale.ROOT))) {
                errors.add("[keys] " + at + ": camelCase key; use '" + camelFix(k) + "'");
            } else if (!SNAKE.matcher(k).matches()) {
                errors.add("[keys] " + at + ": key is not snake_case");
            } else {
                for (String tok : k.split("_")) {
                    String fix = LONG_UNITS.get(tok);
                    if (fix != null) errors.add("[keys] " + at + ": spell the unit as the suffix _" + fix + ", not _" + tok);
                }
            }
            String lower = k.toLowerCase(Locale.ROOT);
            boolean hasUnit = hasUnit(lower);
            if (!unitAbove && !hasUnit && isNumberish(e.getValue()) && namesQuantity(lower))
                errors.add("[units] " + at + ": a measured number needs a unit suffix (_kg, _m, _c, _h, _days, _pct ...)");
            walk(e.getValue(), file, path.isEmpty() ? k : path + "." + k, unitAbove || hasUnit);
        }
    }

    static boolean hasUnit(String key) {
        for (String tok : key.split("_")) if (UNIT_TOKEN.matcher(tok).matches()) return true;
        return false;
    }

    /**
     * True when a key names a measured quantity. A key ending in a dimensionless word ({@code mass_balance_fraction},
     * {@code floor_sediment_multiplier}) is a ratio, and a qualified {@code _weight} ({@code long_weight}) is a blend
     * weight; neither needs a unit.
     */
    static boolean namesQuantity(String key) {
        String[] toks = key.split("_");
        String last = toks[toks.length - 1];
        if (DIMENSIONLESS.contains(last)) return false;
        if (last.equals("weight") && toks.length > 1) return false;
        for (String tok : toks) if (QUANTITY.contains(tok)) return true;
        return false;
    }

    /** A number, an array of numbers, or a {min, max} range of numbers. */
    static boolean isNumberish(JsonElement v) {
        if (v.isJsonPrimitive()) return v.getAsJsonPrimitive().isNumber();
        if (v.isJsonArray()) {
            if (v.getAsJsonArray().isEmpty()) return false;
            for (JsonElement e : v.getAsJsonArray()) if (!isNumberish(e)) return false;
            return true;
        }
        if (v.isJsonObject()) {
            JsonObject o = v.getAsJsonObject();
            if (o.isEmpty() || !Set.of("min", "max").containsAll(o.keySet())) return false;
            for (Map.Entry<String, JsonElement> e : o.entrySet()) if (!isNumberish(e.getValue())) return false;
            return true;
        }
        return false;
    }

    /** Split at capitals, lower-case, turn a trailing unit word into its suffix: daysAt5C becomes days_at_5c. */
    static String camelFix(String k) {
        List<String> parts = new ArrayList<>();
        Matcher m = Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+[A-Za-z]?").matcher(k);
        while (m.find()) parts.add(m.group().toLowerCase(Locale.ROOT));
        if (parts.isEmpty()) return k.toLowerCase(Locale.ROOT);
        String last = parts.get(parts.size() - 1);
        if (CAMEL_UNITS.containsKey(last)) parts.set(parts.size() - 1, CAMEL_UNITS.get(last));
        else if (LONG_UNITS.containsKey(last)) parts.set(parts.size() - 1, LONG_UNITS.get(last));
        return String.join("_", parts);
    }

    // ---- 3: file names ---------------------------------------------------------------------------------------

    void fileNames() throws IOException {
        int n = 0;
        for (String base : List.of("data/yearofice", "assets/yearofice")) {
            for (Path dir : resourceRoots(base)) {
                try (Stream<Path> s = Files.walk(dir)) {
                    for (Path p : s.filter(f -> !f.equals(dir)).sorted().toList()) {
                        n++;
                        String name = p.getFileName().toString();
                        if (!FILE_NAME.matcher(name).matches()) errors.add("[names] " + rel(p) + ": name is not snake_case");
                    }
                }
            }
        }
        checked.put("data and asset paths", n);
    }

    // ---- 4: credits and licences -----------------------------------------------------------------------------

    void credits() throws IOException {
        Path creditsFile = root.resolve("src/main/resources/assets/yearofice/credits.json");
        Set<String> assets = new HashSet<>();
        for (Path dir : resourceRoots("assets/yearofice")) {
            Path resRoot = dir.getParent().getParent();
            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(Files::isRegularFile).forEach(p -> assets.add(resRoot.relativize(p).toString().replace('\\', '/')));
            }
        }
        assets.removeIf(a -> NOT_ART.stream().anyMatch(a::startsWith));
        if (!Files.exists(creditsFile)) {
            if (!assets.isEmpty()) errors.add("[credits] assets exist but assets/yearofice/credits.json does not");
            return;
        }
        JsonObject credits = JsonParser.parseString(Files.readString(creditsFile, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, String> listed = new HashMap<>();
        int entries = 0, found = 0;
        for (JsonElement el : credits.getAsJsonArray("entries")) {
            entries++;
            JsonObject e = el.getAsJsonObject();
            String id = e.has("asset_id") ? e.get("asset_id").getAsString() : "(entry " + entries + ")";
            for (JsonElement f : e.getAsJsonArray("files")) {
                String file = f.getAsString();
                String prev = listed.put(file, id);
                if (prev != null) errors.add("[credits] " + file + ": listed by both " + prev + " and " + id);
                if (!assets.contains(file)) errors.add("[credits] " + id + ": lists " + file + ", which does not exist");
            }
            String licence = e.has("licence") ? e.get("licence").getAsString() : null;
            String type = e.has("source_type") ? e.get("source_type").getAsString() : null;
            if (licence == null || !LICENCES.contains(licence)) errors.add("[credits] " + id + ": licence '" + licence + "' is not on the allowed list");
            if (type == null) { errors.add("[credits] " + id + ": no source_type"); continue; }
            if (type.equals("generated")) {
                if (!"project".equals(licence)) errors.add("[credits] " + id + ": a generated asset carries the 'project' licence");
                continue;
            }
            found++;
            if ("project".equals(licence)) errors.add("[credits] " + id + ": a found asset needs its source's licence, not 'project'");
            String lf = e.has("licence_file") ? e.get("licence_file").getAsString() : null;
            if (lf == null || !Files.isRegularFile(root.resolve(lf))) errors.add("[credits] " + id + ": found asset without its licence file (" + lf + ")");
            if (licence != null && (licence.contains("-SA-") || licence.contains("-NC-"))
                && !(e.has("release_blocker") && e.get("release_blocker").getAsBoolean()))
                errors.add("[credits] " + id + ": " + licence + " needs release_blocker: true");
            if (licence != null && licence.startsWith("CC-BY") && !(e.has("author") && e.has("title")))
                errors.add("[credits] " + id + ": a CC-BY entry needs its credit line (author and title)");
            if (e.has("original")) {
                Path orig = root.resolve(e.get("original").getAsString());
                if (!Files.isRegularFile(orig)) errors.add("[credits] " + id + ": original " + e.get("original").getAsString() + " is missing");
                else if (e.has("original_sha256") && !sha256(Files.readAllBytes(orig)).equals(e.get("original_sha256").getAsString()))
                    errors.add("[credits] " + id + ": original's SHA-256 does not match its entry");
            }
        }
        for (String a : assets) if (!listed.containsKey(a)) errors.add("[credits] " + a + ": no credits.json entry");
        checked.put("asset files", assets.size());
        checked.put("credits entries", entries);
        checked.put("found-asset entries", found);
    }

    // ---- 5: no Mojang assets in the jar ----------------------------------------------------------------------

    void jar(Path modJar, Path vanillaJar) throws IOException {
        if (!Files.isRegularFile(modJar)) { errors.add("[mojang] mod jar " + modJar + " not found; run the jar task first"); return; }
        boolean haveVanilla = vanillaJar != null && Files.isRegularFile(vanillaJar);
        if (!haveVanilla) warnings.add("[mojang] Minecraft resources jar not found (" + vanillaJar + "); checked paths only, not contents");
        try (ZipFile mod = new ZipFile(modJar.toFile())) {
            List<? extends ZipEntry> resources = mod.stream()
                .filter(e -> !e.isDirectory() && (e.getName().startsWith("assets/") || e.getName().startsWith("data/")))
                .toList();
            Map<String, String> vanilla = new HashMap<>();
            if (haveVanilla) {
                // Hash only the vanilla files whose size a mod resource shares; 64 bytes and up, to skip trivial JSON.
                Set<Long> sizes = new HashSet<>();
                resources.forEach(e -> { if (e.getSize() >= 64) sizes.add(e.getSize()); });
                try (ZipFile v = new ZipFile(vanillaJar.toFile())) {
                    for (ZipEntry e : v.stream().map(x -> (ZipEntry) x).toList()) {
                        if (e.isDirectory() || !sizes.contains(e.getSize())) continue;
                        try (InputStream in = v.getInputStream(e)) { vanilla.put(sha256(in.readAllBytes()), e.getName()); }
                    }
                }
            }
            for (ZipEntry e : resources) {
                if (e.getName().startsWith("assets/minecraft/")) errors.add("[mojang] jar holds " + e.getName() + " (no files under assets/minecraft/)");
                if (vanilla.isEmpty() || e.getSize() < 64) continue;
                try (InputStream in = mod.getInputStream(e)) {
                    String v = vanilla.get(sha256(in.readAllBytes()));
                    if (v != null) errors.add("[mojang] jar's " + e.getName() + " is byte-identical to vanilla " + v);
                }
            }
            checked.put("jar resources", resources.size());
            if (haveVanilla) checked.put("vanilla files hashed for comparison", vanilla.size());
        }
    }

    // ---- 6: CODEOWNERS; inventory staleness (warning) --------------------------------------------------------

    void codeowners() throws IOException {
        Path out = root.resolve(".github/CODEOWNERS");
        String want = Codeowners.generate(root);
        if (!Files.exists(out) || !Files.readString(out, StandardCharsets.UTF_8).equals(want))
            errors.add("[codeowners] .github/CODEOWNERS does not match studio/ownership.json; run ./gradlew codeowners");
        checked.put("CODEOWNERS", 1);
    }

    void inventory() throws IOException {
        Path out = root.resolve("studio/inventory.json");
        try {
            DocInventory.Result r = DocInventory.generate(root);
            if (!Files.exists(out) || !Files.readString(out, StandardCharsets.UTF_8).equals(r.json()))
                warnings.add("[inventory] studio/inventory.json differs from the doc and rules; run ./gradlew docInventory and review the diff");
            checked.put("inventory entries", r.entries().size());
        } catch (IllegalStateException e) {
            warnings.add("[inventory] the doc no longer matches studio/inventory-rules.json: " + e.getMessage());
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    /** src/main/resources/<base> and src/generated/resources/<base>, where they exist. */
    private List<Path> resourceRoots(String base) {
        List<Path> out = new ArrayList<>();
        for (String r : List.of("src/main/resources/", "src/generated/resources/")) {
            Path p = root.resolve(r + base);
            if (Files.isDirectory(p)) out.add(p);
        }
        return out;
    }

    private String rel(Path p) { return root.relativize(p).toString().replace('\\', '/'); }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
