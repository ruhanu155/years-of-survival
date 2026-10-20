package com.yearofice.story;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * {@code story.Journal}: the player's own record of what happened to them (Collapse, SOS and the flight, step 6).
 * Entries are append-only and live in the player's persisted data, so they survive death copies and need no new
 * attachment. The year card counts the {@code flight} kind.
 */
public final class Journal {
    private Journal() {}

    public static final String KEY = "yearofice:journal";
    public static final String FLIGHT = "flight";
    public static final String RUN_ENDED = "run_ended";

    public record Entry(String kind, String cause, String line, long minute, int day) {}

    /** Appends one entry. */
    public static void record(ServerPlayer p, String kind, String cause, String line, long minute, int day) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = root.getList(KEY, Tag.TAG_COMPOUND);
        CompoundTag t = new CompoundTag();
        t.putString("kind", kind);
        t.putString("cause", cause);
        t.putString("line", line);
        t.putLong("minute", minute);
        t.putInt("day", day);
        list.add(t);
        root.put(KEY, list);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }

    public static List<Entry> entries(ServerPlayer p) {
        ListTag list = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(KEY, Tag.TAG_COMPOUND);
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new Entry(t.getString("kind"), t.getString("cause"), t.getString("line"), t.getLong("minute"), t.getInt("day")));
        }
        return out;
    }

    /** Flights out: the year card's count. */
    public static long flights(ServerPlayer p) { return entries(p).stream().filter(e -> FLIGHT.equals(e.kind())).count(); }
}
