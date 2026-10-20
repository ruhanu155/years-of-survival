package com.yearofice.story.collection;

import com.yearofice.item.component.TrophySize;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The record book's game page (Waters and fish › The fish record book, A5; RECONCILE 170): per species, the biggest
 * kill's measure, size, z-score, class and day, beside the fish page. The item stays {@code fish_record_book}. Lives in
 * the player's persisted data, like the journal, so it survives death copies.
 */
public final class RecordBook {
    private RecordBook() {}

    public static final String KEY = "yearofice:record_book_game";

    public record Record(String measure, double value, double z, String cls, int day) {}

    /** Writes the entry if it beats the species' record. Returns true when it is a new record. */
    public static boolean record(ServerPlayer p, String species, TrophySize size, int day) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag page = root.getCompound(KEY);
        if (page.contains(species) && page.getCompound(species).getDouble("z") >= size.z()) return false;
        CompoundTag t = new CompoundTag();
        t.putString("measure", size.measure());
        t.putDouble("value", size.value());
        t.putDouble("z", size.z());
        t.putString("class", size.cls());
        t.putInt("day", day);
        page.put(species, t);
        root.put(KEY, page);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        return true;
    }

    public static Map<String, Record> page(ServerPlayer p) {
        CompoundTag page = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
        Map<String, Record> out = new LinkedHashMap<>();
        for (String k : page.getAllKeys()) {
            CompoundTag t = page.getCompound(k);
            out.put(k, new Record(t.getString("measure"), t.getDouble("value"), t.getDouble("z"), t.getString("class"), t.getInt("day")));
        }
        return out;
    }
}
