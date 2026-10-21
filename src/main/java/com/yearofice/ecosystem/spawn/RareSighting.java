package com.yearofice.ecosystem.spawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.sim.ecosystem.Straggler;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Rare sightings (data/yearofice/rare_sighting/*.json): the live spawner for {@link Straggler}. Each year the straggler roll
 * decides whether a yearling buck is in the Interior between his arrival and arrival + stay; while he is, one stands
 * about {@code spawn_distance_m} from a player. ponytail: he stays where he is when the stay ends and wolves are not
 * simulated against him; upgrade: despawn through the abstract tier when it lands (M7).
 */
public final class RareSighting {
    private RareSighting() {}

    public record Def(String id, String species, String flag, boolean male, int startMonth, int startDay, int windowDays,
                      double snowPeakCm, int distanceM) {}

    private static volatile Map<String, Def> DEFS = Map.of();

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new Loader()); }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() { super(new com.google.gson.Gson(), "rare_sighting"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<String, Def> next = new ConcurrentHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
                try {
                    JsonObject o = e.getValue().getAsJsonObject();
                    if (!"straggler".equals(o.get("kind").getAsString())) continue;
                    var ws = o.getAsJsonArray("window_start");
                    next.put(e.getKey().getPath(), new Def(e.getKey().getPath(), o.get("species").getAsString(), o.get("flag").getAsString(),
                            o.get("male").getAsBoolean(), ws.get(0).getAsInt(), ws.get(1).getAsInt(), o.get("window_days").getAsInt(),
                            o.get("winter_snow_peak_cm").getAsDouble(), o.get("spawn_distance_m").getAsInt()));
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("rare_sighting/{}: {}", e.getKey().getPath(), ex.toString());
                }
            }
            DEFS = next;
        }
    }

    public static List<Def> defs() { return List.copyOf(DEFS.values()); }

    /** The year's straggler if he is in the Interior on {@code dayFromStart} (0 = the window's first day), else null. */
    public static Straggler.Year present(Def d, long seed, int year, int dayFromStart) {
        Straggler.Year prev = Straggler.roll(seed, year - 1, false, d.snowPeakCm(), 1);
        Straggler.Year y = Straggler.roll(seed, year, prev.straggler(), d.snowPeakCm(), 1);
        return y.straggler() && dayFromStart >= y.arrivalDay() && dayFromStart < y.arrivalDay() + y.stayDays() ? y : null;
    }

    /** Places him near the player if the roll says he is here and he is not already; returns the body or null. */
    public static YoiAnimal ensure(ServerLevel level, Player p, Def d, long seed, int year, int dayOfYear) {
        if (!EcoConfig.flagOn(d.flag())) return null;
        int start = Calendar.dayOfYear(d.startMonth(), d.startDay());
        int from = dayOfYear - start;
        if (from < 0 || from >= d.windowDays() || present(d, seed, year, from) == null) return null;
        String key = "rare:" + d.id() + ":" + year;
        if (SlotSpawner.has(key)) return null;
        double a = level.random.nextDouble() * Math.PI * 2;
        BlockPos g = Habitats.surface(level, p.getBlockX() + (int) (Math.cos(a) * d.distanceM()), p.getBlockZ() + (int) (Math.sin(a) * d.distanceM()));
        YoiAnimal deer = SlotSpawner.spawn(level, EcosystemRegistry.MULE_DEER.get(), g, key);
        if (deer != null) deer.setMale(d.male());
        return deer;
    }

    /** Called from the slot spawner's 2-second tick; cheap unless a straggler year is on. */
    public static void tick(ServerLevel level) {
        if (DEFS.isEmpty() || level.getServer().getTickCount() % 400 != 0) return;
        long minute = SliceClock.minute(level);
        int year = Calendar.yearOfDay(Calendar.absDayOfMinute(minute)), doy = Calendar.dayOfYearOfMinute(minute);
        for (Player p : level.players()) {
            if (!(p instanceof ServerPlayer) || p.isSpectator()) continue;
            for (Def d : DEFS.values()) ensure(level, p, d, level.getSeed(), year, doy);
        }
    }
}
