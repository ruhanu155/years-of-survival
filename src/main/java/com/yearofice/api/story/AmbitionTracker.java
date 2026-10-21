package com.yearofice.api.story;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Ambitions (Open world in depth › Ambitions): "Beyond" goals that open on a first and close on a measure, with no
 * deadline and no gold. This is the player's ledger of which are open and which are done; each system opens its own
 * and reports its progress here, the Messenger's Ambitions page reads {@link #open} and {@link #done}. Stored on the
 * player's persisted data, so it survives death and the run's end like the Journal.
 */
public final class AmbitionTracker {
    private AmbitionTracker() {}

    /** M6: opens with the first big-game kill (moose, caribou, deer, wolf or a bear). */
    public static final String HUNTING = "beyond_hunting";
    /** M6: opens with the first mount hung on the trophy wall. */
    public static final String TROPHY_WALL = "beyond_trophy_wall";

    static final String KEY = "yearofice:ambitions", DONE_KEY = "yearofice:ambitions_done";

    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG); }

    private static List<String> read(ServerPlayer p, String key) {
        ListTag l = root(p).getList(key, Tag.TAG_STRING);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    private static void write(ServerPlayer p, String key, List<String> ids) {
        CompoundTag r = root(p);
        ListTag l = new ListTag();
        for (String s : ids) l.add(StringTag.valueOf(s));
        r.put(key, l);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, r);
    }

    /** Opens {@code id}; true when it was not open or done already. */
    public static boolean open(ServerPlayer p, String id) {
        List<String> open = read(p, KEY);
        if (open.contains(id) || read(p, DONE_KEY).contains(id)) return false;
        open.add(id);
        write(p, KEY, open);
        return true;
    }

    /** Closes {@code id} as done; true when it was open. */
    public static boolean complete(ServerPlayer p, String id) {
        List<String> open = read(p, KEY);
        if (!open.remove(id)) return false;
        write(p, KEY, open);
        List<String> done = read(p, DONE_KEY);
        done.add(id);
        write(p, DONE_KEY, done);
        return true;
    }

    public static boolean isOpen(ServerPlayer p, String id) { return read(p, KEY).contains(id); }

    public static boolean isDone(ServerPlayer p, String id) { return read(p, DONE_KEY).contains(id); }

    public static List<String> open(ServerPlayer p) { return read(p, KEY); }

    public static List<String> done(ServerPlayer p) { return read(p, DONE_KEY); }
}
