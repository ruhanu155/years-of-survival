package com.yearofice.tools.contentlint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * tools/doc-inventory: builds studio/inventory.json from the build developer's Master registry inventory
 * (Minecraft development › Content pipeline, build and the evolving modpack) and studio/inventory-rules.json.
 *
 * <pre>
 *   ./gradlew docInventory                     writes studio/inventory.json
 *   ./gradlew docInventory --args=--check      fails when the committed file is stale
 * </pre>
 */
public final class DocInventory {
    static final String NS = "yearofice:";
    private static final Pattern LIST = Pattern.compile("^\\*\\*(.+?) \\(([\\d, ]+)\\)\\.\\*\\* (.*)$");
    private static final Pattern MILESTONE = Pattern.compile("\\bM(\\d+)(\\.5)?\\b");

    /** One inventory entry. {@code milestone} is null for a retired id. */
    record Entry(String id, String type, String milestone, String owner, String milestoneSource, String status) {}

    /** The generated file's text and anything the Director should know. */
    record Result(String json, List<String> notes, List<Entry> entries) {}

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 && !args[0].startsWith("--") ? args[0] : ".").toAbsolutePath();
        boolean check = List.of(args).contains("--check");
        Result r = generate(root);
        Path out = root.resolve("studio/inventory.json");
        r.notes().forEach(n -> System.out.println("docInventory: note: " + n));
        if (check) {
            String old = Files.exists(out) ? Files.readString(out, StandardCharsets.UTF_8) : "";
            if (!old.equals(r.json())) {
                System.err.println("docInventory: studio/inventory.json is stale; run ./gradlew docInventory");
                System.exit(1);
            }
            System.out.println("docInventory: studio/inventory.json is current (" + r.entries().size() + " entries)");
            return;
        }
        Files.writeString(out, r.json(), StandardCharsets.UTF_8);
        System.out.println("docInventory: wrote " + root.relativize(out) + " (" + r.entries().size() + " entries)");
    }

    public static Result generate(Path root) throws IOException {
        JsonObject rules = read(root.resolve("studio/inventory-rules.json"));
        Set<String> departments = read(root.resolve("studio/ownership.json")).getAsJsonObject("departments").keySet();
        Set<String> milestones = new LinkedHashSet<>(strings(rules.getAsJsonArray("milestones")));
        List<String> notes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        JsonObject src = rules.getAsJsonObject("source");
        String srcFile = src.get("file").getAsString();
        List<String> inv = Doc.section(Doc.lines(root.resolve(srcFile)), src.get("heading").getAsString(), srcFile);

        // 1. The typed lists, in doc order.
        Map<String, String> listTypes = new LinkedHashMap<>();
        rules.getAsJsonObject("lists").entrySet().forEach(e -> listTypes.put(e.getKey(), e.getValue().getAsString()));
        Set<String> notListed = rules.getAsJsonObject("not_listed").keySet();
        Map<String, Draft> drafts = new LinkedHashMap<>();
        List<String> typeOrder = new ArrayList<>();
        for (String line : inv) {
            Matcher m = LIST.matcher(line);
            if (!m.matches()) continue;
            String name = m.group(1);
            if (notListed.contains(name)) continue;
            String type = listTypes.get(name);
            if (type == null) { errors.add("doc list '" + name + "' has no type in inventory-rules.json lists"); continue; }
            if (!typeOrder.contains(type)) typeOrder.add(type);
            List<String> ids = listIds(m.group(3));
            int header = Integer.parseInt(m.group(2).split(",")[0].trim());
            if (header != ids.size()) notes.add(name + ": the doc's count says " + header + ", its list names " + ids.size());
            for (String id : ids) add(drafts, errors, new Draft(id, type));
        }
        for (String name : listTypes.keySet()) if (drafts.values().stream().noneMatch(d -> d.type.equals(listTypes.get(name)))) errors.add("no doc list found for '" + name + "'");

        // 2. Extras the lists' trailing notes name.
        for (JsonElement el : rules.getAsJsonArray("extras")) {
            JsonObject x = el.getAsJsonObject();
            for (String id : strings(x.getAsJsonArray("ids"))) add(drafts, errors, new Draft(id, x.get("type").getAsString()));
        }

        // 3. The review-round additions, with their milestones and owners.
        Map<String, List<String>> expand = new LinkedHashMap<>();
        rules.getAsJsonObject("expand").entrySet().forEach(e -> expand.put(e.getKey(), strings(e.getValue().getAsJsonArray())));
        Set<String> additionWords = new LinkedHashSet<>();
        expand.values().forEach(additionWords::addAll);
        for (JsonElement el : rules.getAsJsonArray("additions")) {
            JsonObject a = el.getAsJsonObject();
            String type = a.get("type").getAsString();
            if (!typeOrder.contains(type)) typeOrder.add(type);
            for (String raw : strings(a.getAsJsonArray("ids"))) {
                additionWords.add(raw);
                for (String id : expand(raw, expand)) {
                    Draft d = add(drafts, errors, new Draft(id, type));
                    if (d != null) d.set(a.get("milestone").getAsString(), a.get("owner").getAsString(), "doc");
                }
            }
        }
        checkAdditionWords(inv, additionWords, strings(rules.getAsJsonObject("addition_ids_ignored").getAsJsonArray("ids")), errors);

        // 4. Explicit id rules.
        for (JsonElement el : rules.getAsJsonArray("ids")) {
            JsonObject r = el.getAsJsonObject();
            String type = r.get("type").getAsString();
            for (String id : strings(r.getAsJsonArray("ids"))) {
                Draft d = drafts.get(key(type, id));
                if (d == null) { errors.add("ids rule names " + type + " " + id + ", which the inventory does not have (" + r.get("basis").getAsString() + ")"); continue; }
                d.setIfEmpty(opt(r, "milestone"), opt(r, "owner"), "doc");
            }
        }

        // 5. Ids the doc names in a milestone's section.
        for (JsonElement el : rules.getAsJsonArray("sections")) {
            JsonObject s = el.getAsJsonObject();
            String file = s.get("file").getAsString();
            List<String> text = Doc.section(Doc.lines(root.resolve(file)), s.get("heading").getAsString(), file);
            text = slice(text, opt(s, "line_contains"), opt(s, "until_line_contains"));
            Set<String> named = Doc.ids(text);
            Set<String> types = new LinkedHashSet<>(strings(s.getAsJsonArray("types")));
            int hits = 0;
            for (Draft d : drafts.values()) {
                if (types.contains(d.type) && named.contains(d.id)) { d.setIfEmpty(s.get("milestone").getAsString(), null, "doc"); hits++; }
            }
            if (hits == 0) errors.add("section rule '" + s.get("basis").getAsString() + "' matched no entry");
        }

        // 6. The entity registry table (one milestone per row).
        JsonObject et = rules.getAsJsonObject("entity_table");
        String etFile = et.get("file").getAsString();
        for (String row : Doc.section(Doc.lines(root.resolve(etFile)), et.get("heading").getAsString(), etFile)) {
            if (!row.startsWith("| `")) continue;
            String[] cells = row.split("\\|");
            List<String> ids = Doc.backticked(cells[1]);
            String cell = cells[cells.length - 1].isBlank() ? cells[cells.length - 2] : cells[cells.length - 1];
            Set<String> found = new LinkedHashSet<>();
            Matcher mm = MILESTONE.matcher(cell);
            while (mm.find()) found.add("m" + mm.group(1) + (mm.group(2) != null ? "_5" : ""));
            for (String id : ids) {
                for (String type : List.of("mob", "entity")) {
                    Draft d = drafts.get(key(type, id));
                    if (d == null) continue;
                    if (d.milestone == null) {
                        if (found.size() == 1) d.setIfEmpty(found.iterator().next(), null, "doc");
                        else errors.add("entity registry row for " + id + " names " + found + " in '" + cell.trim() + "': add an ids rule");
                    }
                }
            }
        }

        // 7. Patterns, then 8. type defaults.
        List<JsonObject> patterns = new ArrayList<>();
        rules.getAsJsonArray("patterns").forEach(e -> patterns.add(e.getAsJsonObject()));
        JsonObject defaults = rules.getAsJsonObject("type_defaults");
        for (Draft d : drafts.values()) {
            for (JsonObject p : patterns) {
                if (!strings(p.getAsJsonArray("types")).contains(d.type)) continue;
                if (!d.id.matches(p.get("pattern").getAsString())) continue;
                d.setIfEmpty(opt(p, "milestone"), opt(p, "owner"), "rule");
                break;
            }
            JsonObject def = defaults.getAsJsonObject(d.type);
            if (def == null) { errors.add("no type_defaults for " + d.type); continue; }
            d.setIfEmpty(opt(def, "milestone"), opt(def, "owner"), "rule");
        }

        // Status: retired and not-registered ids.
        for (JsonElement el : rules.getAsJsonArray("status")) {
            JsonObject s = el.getAsJsonObject();
            for (String id : strings(s.getAsJsonArray("ids"))) {
                Draft d = drafts.get(key(s.get("type").getAsString(), id));
                if (d == null) { notes.add("status rule names " + s.get("type").getAsString() + " " + id + ", which the inventory does not list"); continue; }
                d.status = s.get("status").getAsString();
                if (d.status.equals("retired")) { d.milestone = null; d.source = "doc"; }
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (String type : typeOrder) {
            drafts.values().stream().filter(d -> d.type.equals(type)).sorted((a, b) -> a.id.compareTo(b.id)).forEach(d -> {
                if (d.milestone != null && !milestones.contains(d.milestone)) errors.add(d.type + " " + d.id + ": unknown milestone " + d.milestone);
                if (d.milestone == null && !"retired".equals(d.status)) errors.add(d.type + " " + d.id + ": no milestone");
                if (d.owner == null || !departments.contains(d.owner)) errors.add(d.type + " " + d.id + ": owner '" + d.owner + "' is not a department in studio/ownership.json");
                entries.add(new Entry(NS + d.id, d.type, d.milestone, d.owner, d.source, d.status));
            });
        }
        if (!errors.isEmpty()) throw new IllegalStateException("docInventory: " + errors.size() + " problem(s):\n  " + String.join("\n  ", errors));
        return new Result(write(entries, srcFile, src.get("heading").getAsString(), notes, milestones), notes, entries);
    }

    /** The ids of one bold list: comma-separated, ending at a semicolon, a full stop or the first non-id word. */
    static List<String> listIds(String body) {
        List<String> out = new ArrayList<>();
        for (String piece : body.split(",")) {
            String p = piece.trim();
            boolean last = false;
            int semi = p.indexOf(';');
            if (semi >= 0) { p = p.substring(0, semi).trim(); last = true; }
            if (p.endsWith(".")) { p = p.substring(0, p.length() - 1); last = true; }
            if (!Doc.ID.matcher(p).matches()) break;
            out.add(p);
            if (last) break;
        }
        return out;
    }

    private static void checkAdditionWords(List<String> inv, Set<String> covered, List<String> ignored, List<String> errors) {
        boolean in = false;
        for (String line : inv) {
            if (line.startsWith("**Added in the review round")) { in = true; continue; }
            if (!in) continue;
            if (!line.startsWith("- ")) { if (!line.isBlank()) in = false; continue; }
            for (String w : Doc.backticked(line)) {
                String bare = w.startsWith("yearofice:") ? w.substring(10) : w;
                if (!covered.contains(w) && !covered.contains(bare) && !ignored.contains(w))
                    errors.add("review-round word `" + w + "` is neither an addition nor in addition_ids_ignored");
            }
        }
    }

    private static List<String> expand(String raw, Map<String, List<String>> expand) {
        for (Map.Entry<String, List<String>> e : expand.entrySet()) {
            if (raw.contains(e.getKey())) {
                List<String> out = new ArrayList<>();
                for (String v : e.getValue()) out.add(raw.replace(e.getKey(), v));
                return out;
            }
        }
        return List.of(raw);
    }

    private static List<String> slice(List<String> text, String from, String until) {
        if (from == null || from.isEmpty()) return text;
        int a = -1;
        for (int i = 0; i < text.size(); i++) if (text.get(i).contains(from)) { a = i; break; }
        if (a < 0) throw new IllegalStateException("line containing '" + from + "' not found");
        if (until == null) return text.subList(a, a + 1);
        int b = text.size();
        for (int i = a + 1; i < text.size(); i++) if (text.get(i).contains(until)) { b = i; break; }
        return text.subList(a, b);
    }

    private static String write(List<Entry> entries, String srcFile, String heading, List<String> notes, Set<String> milestones) {
        Map<String, Integer> byType = new LinkedHashMap<>(), byMilestone = new LinkedHashMap<>(), byOwner = new TreeMap<>(), bySource = new TreeMap<>(), byStatus = new TreeMap<>();
        for (String m : milestones) byMilestone.put(m, 0);
        for (Entry e : entries) {
            byType.merge(e.type(), 1, Integer::sum);
            byMilestone.merge(e.milestone() == null ? "none" : e.milestone(), 1, Integer::sum);
            byOwner.merge(e.owner(), 1, Integer::sum);
            bySource.merge(e.milestoneSource(), 1, Integer::sum);
            byStatus.merge(e.status(), 1, Integer::sum);
        }
        byMilestone.values().removeIf(v -> v == 0);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"schema\": 1,\n");
        sb.append("  \"note\": \"Generated by ./gradlew docInventory (tools/content-lint DocInventory) from the doc and studio/inventory-rules.json. Do not edit by hand: change the rules or the doc and regenerate. milestone_source 'doc' means the doc names the milestone; 'rule' means a pattern or type default in the rules file chose it.\",\n");
        sb.append("  \"source\": ").append(q(srcFile + " > " + heading)).append(",\n");
        sb.append("  \"doc_notes\": [");
        for (int i = 0; i < notes.size(); i++) sb.append(i == 0 ? "\n    " : ",\n    ").append(q(notes.get(i)));
        sb.append(notes.isEmpty() ? "],\n" : "\n  ],\n");
        sb.append("  \"counts\": {\n");
        sb.append("    \"total\": ").append(entries.size()).append(",\n");
        sb.append("    \"by_type\": ").append(map(byType)).append(",\n");
        sb.append("    \"by_milestone\": ").append(map(byMilestone)).append(",\n");
        sb.append("    \"by_owner\": ").append(map(byOwner)).append(",\n");
        sb.append("    \"by_milestone_source\": ").append(map(bySource)).append(",\n");
        sb.append("    \"by_status\": ").append(map(byStatus)).append("\n");
        sb.append("  },\n");
        sb.append("  \"entries\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            sb.append("    {\"id\": ").append(q(e.id())).append(", \"type\": ").append(q(e.type()))
                .append(", \"milestone\": ").append(e.milestone() == null ? "null" : q(e.milestone()))
                .append(", \"owner\": ").append(q(e.owner()))
                .append(", \"milestone_source\": ").append(q(e.milestoneSource()))
                .append(", \"status\": ").append(q(e.status())).append("}")
                .append(i + 1 < entries.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static String map(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            sb.append(first ? "" : ", ").append(q(e.getKey())).append(": ").append(e.getValue());
            first = false;
        }
        return sb.append("}").toString();
    }

    static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Draft add(Map<String, Draft> drafts, List<String> errors, Draft d) {
        if (drafts.containsKey(key(d.type, d.id))) { errors.add("duplicate " + d.type + " " + d.id); return null; }
        drafts.put(key(d.type, d.id), d);
        return d;
    }

    private static String key(String type, String id) { return type + " " + id; }

    static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList<>();
        a.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private static String opt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    private static final class Draft {
        final String id, type;
        String milestone, owner, source, status = "planned";

        Draft(String id, String type) { this.id = id; this.type = type; }

        void set(String m, String o, String src) { milestone = m; owner = o; source = src; }

        void setIfEmpty(String m, String o, String src) {
            if (milestone == null && m != null) { milestone = m; source = src; }
            if (owner == null && o != null) owner = o;
        }
    }
}
