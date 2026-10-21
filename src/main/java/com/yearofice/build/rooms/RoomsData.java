package com.yearofice.build.rooms;

import com.yearofice.api.build.Room;
import com.yearofice.sim.build.RoomScan;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_rooms}: the summary of every room found (seed, volume, conductance), so a loaded level
 * knows its rooms at once. The cell sets are transient and rebuilt by the first scan.
 */
public final class RoomsData extends SavedData {
    static final SavedData.Factory<RoomsData> FACTORY = new SavedData.Factory<>(RoomsData::new, RoomsData::load, null);

    /** A live room: its scan, last computed balance and when. */
    static final class Entry {
        RoomScan.Result scan;
        Room room;
        long stamp;
        int minX, minY, minZ, maxX, maxY, maxZ;

        boolean byCellHas(int x, int y, int z) { return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ && scan.air().contains(RoomScan.pack(x, y, z)); }
    }

    final Map<Long, Entry> byCell = new HashMap<>();
    final Map<Long, Long> open = new HashMap<>();
    final ArrayDeque<BlockPos> dirty = new ArrayDeque<>();
    /** Saved summaries: seed (packed) → {volume, uWPerK}. */
    final Map<Long, double[]> summary = new HashMap<>();

    public static RoomsData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, "yearofice_rooms"); }

    void remember(BlockPos seed, int volume, double u) {
        double[] old = summary.put(seed.asLong(), new double[] {volume, u});
        if (old == null || old[0] != volume || Math.abs(old[1] - u) > 0.5) setDirty();
    }

    public int count() { return summary.size(); }

    static RoomsData load(CompoundTag tag, HolderLookup.Provider regs) {
        RoomsData d = new RoomsData();
        for (Tag t : tag.getList("rooms", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.summary.put(c.getLong("seed"), new double[] {c.getInt("volume"), c.getDouble("u")});
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        ListTag l = new ListTag();
        summary.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("seed", k);
            c.putInt("volume", (int) v[0]);
            c.putDouble("u", v[1]);
            l.add(c);
        });
        tag.put("rooms", l);
        return tag;
    }
}
