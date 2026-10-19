package com.yearofice.story.clock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_clock} (The save and sync matrix): {@code game_ds} and the world's day length.
 *
 * <p>The day length belongs in {@code yearofice_world} (RECONCILE 117), which does not exist yet; it lives here
 * until it does, taken once from {@code [general] day_length_min} when the world is first loaded. The record is
 * version 1 with no {@code v} field, the versioned-codec rule's starting point (Saves across versions); a save
 * with a {@code v} above {@link #VERSION} is refused rather than read wrong.
 */
public final class ClockData extends SavedData {
    public static final String ID = "yearofice_clock";
    public static final int VERSION = 1;

    long gameDs;
    int dayLengthMin;

    ClockData(long gameDs, int dayLengthMin) {
        this.gameDs = gameDs;
        this.dayLengthMin = dayLengthMin;
    }

    public long gameDs() { return gameDs; }

    public int dayLengthMin() { return dayLengthMin; }

    public static ClockData load(CompoundTag tag, HolderLookup.Provider registries) {
        int v = tag.contains("v") ? tag.getInt("v") : 1;
        if (v > VERSION) {
            throw new IllegalStateException("yearofice_clock was saved by a newer Year of Ice (record v" + v + "); update the mod to open this world");
        }
        long ds = tag.getLong("game_ds");
        int len = tag.contains("day_length_min") ? tag.getInt("day_length_min") : 24;
        return new ClockData(Math.max(0, ds), len);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("game_ds", gameDs);
        tag.putInt("day_length_min", dayLengthMin);
        return tag;
    }
}
