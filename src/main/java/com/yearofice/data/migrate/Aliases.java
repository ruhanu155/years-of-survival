package com.yearofice.data.migrate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Id aliases for retired ids (Saves across versions): an id is never renamed or re-used; the old id stays mapped to
 * the new one forever. Aliases are listed per kind ({@code block}, {@code item}, {@code entity}) in
 * {@code data/yearofice/retired/<kind>.json} as {@code { "kind": "item", "aliases": { "yearofice:old": "yearofice:new" } }}.
 * {@link #onRegister} hands them to the game registries (the same call {@code DeferredRegister#addAlias} makes), and
 * {@link #resolve} serves ids saved as plain strings in a record.
 */
public final class Aliases {
    public static final List<String> KINDS = List.of("block", "item", "entity");
    private static final int MAX_CHAIN = 16;

    private final Map<String, Map<String, String>> byKind = new HashMap<>();
    private static volatile Aliases shared;

    public static Aliases shared() {
        Aliases a = shared;
        if (a == null) {
            a = new Aliases();
            for (String kind : KINDS) {
                try (InputStream in = Aliases.class.getResourceAsStream("/data/yearofice/retired/" + kind + ".json")) {
                    if (in != null) a.addJson(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (IOException e) {
                    throw new IllegalStateException("cannot read retired/" + kind + ".json", e);
                }
            }
            shared = a;
        }
        return a;
    }

    public Aliases add(String kind, String from, String to) {
        if (from.equals(to)) throw new IllegalArgumentException(kind + " alias " + from + " points at itself");
        if (byKind.computeIfAbsent(kind, k -> new TreeMap<>()).putIfAbsent(from, to) != null)
            throw new IllegalArgumentException(kind + " alias " + from + " is listed twice");
        return this;
    }

    public Aliases addJson(JsonObject root) {
        String kind = root.get("kind").getAsString();
        if (root.has("aliases"))
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("aliases").entrySet()) add(kind, e.getKey(), e.getValue().getAsString());
        return this;
    }

    public Map<String, String> of(String kind) { return byKind.getOrDefault(kind, Map.of()); }

    public int count() { return byKind.values().stream().mapToInt(Map::size).sum(); }

    /** Follows the chain old, older, ... to the live id; an id with no alias comes back as it is. */
    public String resolve(String kind, String id) {
        Map<String, String> m = of(kind);
        String cur = id;
        for (int i = 0; i < MAX_CHAIN; i++) {
            String next = m.get(cur);
            if (next == null) return cur;
            cur = next;
        }
        throw new IllegalStateException(kind + " alias chain from " + id + " does not end (a cycle?)");
    }

    /** Every alias resolves (no cycles); returns the problems, empty when sound. */
    public List<String> problems() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (Map.Entry<String, Map<String, String>> k : byKind.entrySet())
            for (String from : k.getValue().keySet()) {
                try { resolve(k.getKey(), from); } catch (IllegalStateException e) { out.add(e.getMessage()); }
            }
        return out;
    }

    /** Mod-bus {@code RegisterEvent}: gives each registry its aliases, as {@code DeferredRegister#addAlias} would. */
    public static void onRegister(RegisterEvent event) {
        String kind = kindOf(event.getRegistryKey());
        if (kind == null) return;
        Registry<?> reg = event.getRegistry();
        shared().of(kind).forEach((from, to) -> reg.addAlias(ResourceLocation.parse(from), ResourceLocation.parse(to)));
    }

    static String kindOf(net.minecraft.resources.ResourceKey<? extends Registry<?>> key) {
        if (key.equals(Registries.BLOCK)) return "block";
        if (key.equals(Registries.ITEM)) return "item";
        if (key.equals(Registries.ENTITY_TYPE)) return "entity";
        return null;
    }
}
