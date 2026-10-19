package com.yearofice.tools.contentlint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates .github/CODEOWNERS from studio/ownership.json. Rules are written shallow to deep, because the last
 * matching CODEOWNERS line wins; each rule carries its department as the comment above it.
 *
 * <pre>
 *   ./gradlew codeowners                  writes .github/CODEOWNERS
 *   ./gradlew codeowners --args=--check   fails when the committed file is stale (contentLint runs this)
 * </pre>
 */
public final class Codeowners {
    private Codeowners() {}

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 && !args[0].startsWith("--") ? args[0] : ".").toAbsolutePath();
        Path out = root.resolve(".github/CODEOWNERS");
        String text = generate(root);
        if (List.of(args).contains("--check")) {
            if (!Files.exists(out) || !Files.readString(out, StandardCharsets.UTF_8).equals(text)) {
                System.err.println("codeowners: .github/CODEOWNERS is stale; run ./gradlew codeowners");
                System.exit(1);
            }
            return;
        }
        Files.createDirectories(out.getParent());
        Files.writeString(out, text, StandardCharsets.UTF_8);
        System.out.println("codeowners: wrote .github/CODEOWNERS");
    }

    record Rule(String pattern, String department) {}

    static String generate(Path root) throws IOException {
        JsonObject own = DocInventory.read(root.resolve("studio/ownership.json"));
        JsonObject depts = own.getAsJsonObject("departments");
        List<Rule> rules = new ArrayList<>();
        for (JsonElement el : own.getAsJsonArray("packages")) {
            JsonObject p = el.getAsJsonObject();
            String dir = p.get("package").getAsString().replace('.', '/') + "/";
            for (String set : List.of("main", "test")) rules.add(new Rule("/src/" + set + "/java/" + dir, p.get("department").getAsString()));
        }
        for (JsonElement el : own.getAsJsonArray("data")) {
            JsonObject d = el.getAsJsonObject();
            String path = d.get("path").getAsString();
            String suffix = path.endsWith(".json") ? "" : "/";
            for (String set : List.of("main", "generated")) rules.add(new Rule("/src/" + set + "/resources/data/yearofice/" + path + suffix, d.get("department").getAsString()));
        }
        for (JsonElement el : own.getAsJsonArray("paths")) {
            JsonObject p = el.getAsJsonObject();
            rules.add(new Rule("/" + p.get("path").getAsString(), p.get("department").getAsString()));
        }
        rules.sort(Comparator.comparingInt((Rule r) -> depth(r.pattern())).thenComparing(Rule::pattern));

        String fallback = own.get("default_owner").getAsString();
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated from studio/ownership.json by ./gradlew codeowners. Do not edit by hand.\n");
        sb.append("# Rules run shallow to deep because the last match wins. The comment above each rule names its department.\n\n");
        sb.append("* ").append(fallback).append("\n");
        for (Rule r : rules) {
            JsonObject d = depts.getAsJsonObject(r.department());
            if (d == null) throw new IllegalStateException("ownership.json: unknown department " + r.department() + " for " + r.pattern());
            List<String> owners = DocInventory.strings(d.getAsJsonArray("github"));
            sb.append("\n# ").append(r.department()).append(": ").append(d.get("label").getAsString()).append("\n");
            sb.append(r.pattern()).append(" ").append(String.join(" ", owners)).append("\n");
        }
        return sb.toString();
    }

    private static int depth(String pattern) {
        String p = pattern.endsWith("/") ? pattern.substring(0, pattern.length() - 1) : pattern;
        return (int) p.chars().filter(c -> c == '/').count();
    }
}
