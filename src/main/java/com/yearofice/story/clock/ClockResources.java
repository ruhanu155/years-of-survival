package com.yearofice.story.clock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Rates;
import com.yearofice.sim.time.Sun;
import com.yearofice.sim.weather.ClimateJson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads the clock's two data files: {@code clock/rates.json} and the daylight table in
 * {@code weather/interior_alaska.json}. The copies shipped in the jar are read at class load, so the clock runs
 * before any data pack has loaded (and in JUnit-free GameTest set-up); a server reload replaces them.
 */
final class ClockResources extends SimplePreparableReloadListener<ClockResources.Loaded> {
    record Loaded(Rates rates, Sun sun) {}

    static final ResourceLocation RATES = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "clock/rates.json");
    static final ResourceLocation WEATHER = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "weather/interior_alaska.json");

    static volatile Loaded current = fromJar();

    static Loaded fromJar() {
        return new Loaded(rates(jar("/data/yearofice/clock/rates.json")), ClimateJson.parse(jar("/data/yearofice/weather/interior_alaska.json")).sun());
    }

    private static JsonObject jar(String path) {
        try (InputStream in = ClockResources.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    static Rates rates(JsonObject o) {
        JsonObject d = o.getAsJsonObject("ds_per_tick");
        var lens = o.getAsJsonArray("allowed_lengths_min");
        int[] l = new int[lens.size()];
        for (int i = 0; i < l.length; i++) l[i] = lens.get(i).getAsInt();
        return new Rates(d.get("close").getAsInt(), d.get("travel").getAsInt(), d.get("world_at_l24").getAsInt(),
                d.get("work").getAsInt(), d.get("until_done").getAsInt(), d.get("sleep").getAsInt(), l,
                o.has("first_fire_world_pct") ? o.get("first_fire_world_pct").getAsInt() : Rates.DEFAULT_DAY_ONE_PCT);
    }

    @Override
    protected Loaded prepare(ResourceManager rm, ProfilerFiller profiler) {
        try {
            Rates r = read(rm, RATES).map(ClockResources::rates).orElse(current.rates());
            Sun s = read(rm, WEATHER).map(j -> ClimateJson.parse(j).sun()).orElse(current.sun());
            return new Loaded(r, s);
        } catch (RuntimeException e) {
            YearOfIce.LOG.error("Clock data failed to load; keeping the previous rates and daylight table", e);
            return current;
        }
    }

    @Override
    protected void apply(Loaded loaded, ResourceManager rm, ProfilerFiller profiler) { current = loaded; }

    static Optional<JsonObject> read(ResourceManager rm, ResourceLocation id) {
        Optional<Resource> res = rm.getResource(id);
        if (res.isEmpty()) return Optional.empty();
        try (Reader r = res.get().openAsReader()) {
            return Optional.of(JsonParser.parseReader(r).getAsJsonObject());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(id.toString(), e);
        }
    }
}
