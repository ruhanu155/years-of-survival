package com.yearofice.world.season;

import com.yearofice.sim.world.SeasonCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_ice}: today's snow packs, ice thickness per body class, the flood pulse and the open
 * ice holes with the game minute each was cut. One per server (the overworld's store); the passes read it, only
 * {@link SeasonSystem} advances it (one writer).
 */
public final class SeasonState extends SavedData {
    public static final String ID = "yearofice_ice";
    static final SavedData.Factory<SeasonState> FACTORY = new SavedData.Factory<>(SeasonState::new, SeasonState::load, null);

    public final SeasonCore core = new SeasonCore();
    /** Open ice holes: block position (packed) to {last checked game minute, skin progress in thousandths}. */
    public final Map<Long, long[]> holes = new LinkedHashMap<>();

    public static SeasonState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public static SeasonState load(CompoundTag tag, HolderLookup.Provider regs) {
        SeasonState s = new SeasonState();
        long[] bits = tag.getLongArray("core");
        if (bits.length > 0) {
            double[] a = new double[bits.length];
            for (int i = 0; i < a.length; i++) a[i] = Double.longBitsToDouble(bits[i]);
            s.core.decode(a);
        }
        s.core.lastDay = tag.contains("last_day") ? tag.getLong("last_day") : Long.MIN_VALUE;
        long[] h = tag.getLongArray("holes");
        for (int i = 0; i + 2 < h.length; i += 3) s.holes.put(h[i], new long[] {h[i + 1], h[i + 2]});
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        double[] a = core.encode();
        long[] bits = new long[a.length];
        for (int i = 0; i < a.length; i++) bits[i] = Double.doubleToLongBits(a[i]);
        tag.putLongArray("core", bits);
        if (core.lastDay != Long.MIN_VALUE) tag.putLong("last_day", core.lastDay);
        long[] h = new long[holes.size() * 3];
        int i = 0;
        for (Map.Entry<Long, long[]> e : holes.entrySet()) { h[i++] = e.getKey(); h[i++] = e.getValue()[0]; h[i++] = e.getValue()[1]; }
        tag.putLongArray("holes", h);
        return tag;
    }
}
