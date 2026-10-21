package com.yearofice.plant.fungi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.life.MigrationDef;
import com.yearofice.sim.plant.PlantPhase;
import com.yearofice.sim.time.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Seasonal appearance windows (Plants and fungi › Fungi, Aquatic plants): {@code data/yearofice/fungus/<id>.json} and
 * {@code data/yearofice/aquatic/<id>.json} give the dates a species shows. A mushroom fruits only inside its window (and
 * only once the season's degree-days have reached its fruit phase); an aquatic plant is {@code dormant} under the ice outside
 * its window. {@link com.yearofice.plant.PlantPhasePass#phaseNow} passes every phase through {@link #gate}. Species with no
 * window row are untouched. Pure lookup; the listeners fill the map on reload.
 */
public class SeasonWindows extends SimpleJsonResourceReloadListener {
    /** One window in game-year days (May 1 is day 0); {@code outside} is the phase shown when it is closed. */
    public record Window(String id, int from, int peak, int to, PlantPhase outside) {
        public boolean open(int doy) { return Calendar.within(Math.floorMod(doy, Calendar.DAYS_PER_YEAR), from, to); }
    }

    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<>();
    private final String folder;

    protected SeasonWindows(String folder) {
        super(new Gson(), folder);
        this.folder = folder;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        WINDOWS.keySet().removeIf(k -> k.startsWith(folder + "/"));
        for (var e : in.entrySet()) {
            if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
            try {
                Window w = parse(e.getValue().getAsJsonObject());
                WINDOWS.put(folder + "/" + w.id(), w);
            } catch (RuntimeException ex) {
                YearOfIce.LOG.warn("Bad {} window {}: {}", folder, e.getKey(), ex.toString());
            }
        }
    }

    public static Window parse(JsonObject o) {
        JsonObject w = o.getAsJsonObject("window");
        int from = MigrationDef.day(w.get("from").getAsString()), to = MigrationDef.day(w.get("to").getAsString());
        int peak = w.has("peak") ? MigrationDef.day(w.get("peak").getAsString()) : (from + to) / 2;
        PlantPhase outside = PlantPhase.of(o.has("outside") ? o.get("outside").getAsString() : "leaf");
        return new Window(o.get("id").getAsString(), from, peak, to, outside);
    }

    public static Window get(String speciesId) {
        Window w = WINDOWS.get("fungus/" + speciesId);
        return w != null ? w : WINDOWS.get("aquatic/" + speciesId);
    }

    public static int count() { return WINDOWS.size(); }

    /** The phase to show on {@code doy}: the degree-day phase, unless the species' window is closed. */
    public static PlantPhase gate(String speciesId, int doy, PlantPhase phase) {
        Window w = get(speciesId);
        if (w == null || w.open(doy)) return phase;
        return switch (w.outside()) {
            case LEAF -> phase == PlantPhase.FRUIT || phase == PlantPhase.SEED || phase == PlantPhase.FLOWER ? PlantPhase.LEAF : phase;
            default -> w.outside();
        };
    }
}
