package com.yearofice.env;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.weather.Climate;
import com.yearofice.sim.weather.ClimateJson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads {@code weather/interior_alaska.json} into the {@link Climate}; the jar's copy until data packs load. */
final class EnvResources extends SimplePreparableReloadListener<Climate> {
    static final ResourceLocation WEATHER = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "weather/interior_alaska.json");

    static volatile Climate current = fromJar();

    static Climate fromJar() {
        try (InputStream in = EnvResources.class.getResourceAsStream("/data/yearofice/weather/interior_alaska.json")) {
            if (in == null) throw new IllegalStateException("missing weather/interior_alaska.json");
            return ClimateJson.parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()).climate();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Climate prepare(ResourceManager rm, ProfilerFiller profiler) {
        var res = rm.getResource(WEATHER);
        if (res.isEmpty()) return current;
        try (Reader r = res.get().openAsReader()) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return ClimateJson.parse(o).climate();
        } catch (Exception e) {
            YearOfIce.LOG.error("weather/interior_alaska.json failed to load; keeping the previous climate", e);
            return current;
        }
    }

    @Override
    protected void apply(Climate c, ResourceManager rm, ProfilerFiller profiler) { current = c; }
}
