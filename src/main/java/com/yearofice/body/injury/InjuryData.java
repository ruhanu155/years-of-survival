package com.yearofice.body.injury;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.injury.SourceJson;
import com.yearofice.sim.body.part.InjurySource;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The injury lane's server data: {@code injury_source/}, {@code injury/}, {@code action/treat_*} and
 * {@code body/damage_routes.json} (the sanctioned fallback for the damage-type data map, Body tab › Damage types and
 * tags). Server data only: the client never reads it.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class InjuryData {
    private InjuryData() {}

    private static volatile Map<String, InjurySource> sources = Map.of();
    private static volatile Map<String, JsonObject> injuries = Map.of();
    private static volatile Map<String, JsonObject> actions = Map.of();
    private static volatile Map<String, String> routes = Map.of();

    /** The source named {@code id}, or null. */
    public static @Nullable InjurySource source(String id) { return sources.get(id); }

    public static InjurySource source(String id, InjurySource fallback) {
        InjurySource s = sources.get(id);
        return s == null ? fallback : s;
    }

    public static int sourceCount() { return sources.size(); }

    public static @Nullable JsonObject injury(String id) { return injuries.get(id); }

    /** A {@code treat_<id>} action file's body, or null. */
    public static @Nullable JsonObject action(String id) { return actions.get(id); }

    public static int actionCount() { return actions.size(); }

    /** The route for a damage type id ({@code minecraft:fall}): a source id, or {@code #fall}/{@code #systemic}/{@code #none}. */
    public static @Nullable String route(String damageType) { return routes.get(damageType); }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(new Loader("injury_source", (files) -> {
            Map<String, InjurySource> next = new HashMap<>();
            files.forEach((k, v) -> {
                try {
                    next.put(k.getPath(), SourceJson.parse(k.getPath(), v.getAsJsonObject()));
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("injury_source/{}: {}", k.getPath(), ex.toString());
                }
            });
            sources = Map.copyOf(next);
        }));
        e.addListener(new Loader("injury", (files) -> {
            Map<String, JsonObject> next = new HashMap<>();
            files.forEach((k, v) -> next.put(k.getPath(), v.getAsJsonObject()));
            injuries = Map.copyOf(next);
        }));
        e.addListener(new Loader("action", (files) -> {
            Map<String, JsonObject> next = new HashMap<>();
            files.forEach((k, v) -> {
                if (k.getPath().startsWith("treat_")) next.put(k.getPath().substring("treat_".length()), v.getAsJsonObject());
            });
            actions = Map.copyOf(next);
        }));
        e.addListener(new Loader("body", (files) -> {
            Map<String, String> next = new HashMap<>();
            files.forEach((k, v) -> {
                if (!k.getPath().equals("damage_routes")) return;
                for (Map.Entry<String, JsonElement> r : v.getAsJsonObject().getAsJsonObject("routes").entrySet()) {
                    next.put(r.getKey(), r.getValue().getAsString());
                }
            });
            routes = Map.copyOf(next);
        }));
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private final java.util.function.Consumer<Map<ResourceLocation, JsonElement>> apply;

        Loader(String dir, java.util.function.Consumer<Map<ResourceLocation, JsonElement>> apply) {
            super(new Gson(), dir);
            this.apply = apply;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<ResourceLocation, JsonElement> mine = new HashMap<>();
            files.forEach((k, v) -> {
                if (k.getNamespace().equals(YearOfIce.MOD_ID) && v.isJsonObject()) mine.put(k, v);
            });
            apply.accept(mine);
        }
    }
}
