package com.yearofice.hud.fieldnote;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads {@code data/yearofice/field_note/<id>.json}: trigger type and id, two lang keys and a cited source. */
public final class FieldNoteDefs extends SimpleJsonResourceReloadListener {
    public record Def(String id, String triggerType, String triggerId, String factKey, String ruleKey, String source) {}

    private static volatile Map<String, Def> byId = Map.of();
    private static volatile Map<String, List<Def>> byTrigger = Map.of();

    public FieldNoteDefs() { super(new Gson(), "field_note"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, Def> ids = new HashMap<>();
        Map<String, List<Def>> trig = new HashMap<>();
        for (var e : in.entrySet()) {
            try {
                JsonObject o = GsonHelper.convertToJsonObject(e.getValue(), "field_note");
                JsonObject t = GsonHelper.getAsJsonObject(o, "trigger");
                Def d = new Def(e.getKey().toString(), GsonHelper.getAsString(t, "type"), GsonHelper.getAsString(t, "id", ""),
                        GsonHelper.getAsString(o, "fact_key"), GsonHelper.getAsString(o, "rule_key"),
                        GsonHelper.getAsString(o, "source"));
                ids.put(d.id(), d);
                trig.computeIfAbsent(d.triggerType() + "|" + d.triggerId(), k -> new ArrayList<>()).add(d);
            } catch (RuntimeException ex) {
                YearOfIce.LOG.warn("Bad field note {}: {}", e.getKey(), ex.getMessage());
            }
        }
        byId = ids;
        byTrigger = trig;
    }

    public static Def get(String id) { return byId.get(id); }

    public static List<Def> forTrigger(String type, String id) { return byTrigger.getOrDefault(type + "|" + id, Collections.emptyList()); }

    public static int count() { return byId.size(); }
}
