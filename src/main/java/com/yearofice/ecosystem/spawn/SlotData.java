package com.yearofice.ecosystem.spawn;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_slots}: which population slots are empty, and until which game minute. The slots
 * themselves are recomputed from the seed and the blocks, so this is the only state the spawner saves (the first,
 * smaller form of {@code yearofice_populations}).
 */
public final class SlotData extends SavedData {
    final Map<String, Long> emptyUntil = new HashMap<>();

    public static SlotData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SlotData::new, SlotData::load, null), "yearofice_slots");
    }

    static SlotData load(CompoundTag tag, HolderLookup.Provider regs) {
        SlotData d = new SlotData();
        CompoundTag e = tag.getCompound("empty_until");
        for (String k : e.getAllKeys()) d.emptyUntil.put(k, e.getLong(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        CompoundTag e = new CompoundTag();
        emptyUntil.forEach(e::putLong);
        tag.put("empty_until", e);
        return tag;
    }

    public boolean empty(String key, long minute) {
        Long u = emptyUntil.get(key);
        return u != null && minute < u;
    }

    public void harvest(String key, long untilMinute) {
        emptyUntil.put(key, untilMinute);
        emptyUntil.entrySet().removeIf(e -> e.getValue() < untilMinute - 60L * 1440);
        setDirty();
    }
}
