package com.yearofice.env.weather;

import com.yearofice.sim.weather.StormKind;
import com.yearofice.sim.weather.StormTable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_weather} (Weather and storms › State, sync and events): the storms a debug command or the
 * decade draw forced onto the table, and the storm log for the journal. The year table itself is memory only, rebuilt
 * from the seed. Its writer is {@code env.weather}.
 */
public final class WeatherData extends SavedData {
    static final SavedData.Factory<WeatherData> FACTORY = new SavedData.Factory<>(WeatherData::new, WeatherData::load, null);
    static final int LOG_MAX = 64;

    public record Log(String id, long start, long peak, long end) {}

    private final List<StormTable.Event> forced = new ArrayList<>();
    private final List<Log> log = new ArrayList<>();
    private int version;

    private static volatile WeatherData cached;
    private static volatile net.minecraft.server.MinecraftServer cachedFor;

    /** Cached per server: the env hour is asked for from several threads, the data storage is not safe for that. */
    public static WeatherData get(ServerLevel level) {
        net.minecraft.server.MinecraftServer server = level.getServer();
        WeatherData d = cached;
        if (d != null && cachedFor == server) return d;
        synchronized (WeatherData.class) {
            if (cached == null || cachedFor != server) {
                cached = server.overworld().getDataStorage().computeIfAbsent(FACTORY, "yearofice_weather");
                cachedFor = server;
            }
            return cached;
        }
    }

    static void forget() {
        cached = null;
        cachedFor = null;
    }

    public List<StormTable.Event> forced() { return List.copyOf(forced); }

    public List<Log> log() { return List.copyOf(log); }

    /** Changes whenever the forced list does, so cached tables know to rebuild. */
    public int version() { return version; }

    void force(StormTable.Event e) {
        forced.add(e);
        version++;
        setDirty();
    }

    void clearForced() {
        forced.clear();
        version++;
        setDirty();
    }

    void log(Log l) {
        log.add(l);
        while (log.size() > LOG_MAX) log.remove(0);
        setDirty();
    }

    public static WeatherData load(CompoundTag tag, HolderLookup.Provider lookup) {
        WeatherData d = new WeatherData();
        for (Tag t : tag.getList("forced", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            String id = c.getString("id");
            d.forced.add(new StormTable.Event(id, kindOf(id), c.getLong("start"), c.getInt("hours"), c.getDouble("intensity"), true));
        }
        for (Tag t : tag.getList("log", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.log.add(new Log(c.getString("id"), c.getLong("start"), c.getLong("peak"), c.getLong("end")));
        }
        return d;
    }

    static StormKind kindOf(String id) {
        try {
            return StormKind.of(id);
        } catch (IllegalArgumentException e) {
            return StormKind.CLEAR;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        ListTag f = new ListTag();
        for (StormTable.Event e : forced) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.id());
            c.putLong("start", e.startHour());
            c.putInt("hours", e.hours());
            c.putDouble("intensity", e.intensity());
            f.add(c);
        }
        tag.put("forced", f);
        ListTag l = new ListTag();
        for (Log g : log) {
            CompoundTag c = new CompoundTag();
            c.putString("id", g.id());
            c.putLong("start", g.start());
            c.putLong("peak", g.peak());
            c.putLong("end", g.end());
            l.add(c);
        }
        tag.put("log", l);
        return tag;
    }
}
