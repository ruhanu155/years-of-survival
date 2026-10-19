package com.yearofice.world.attach;

import com.yearofice.sim.wood.TreeMarks;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.Arrays;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;

/**
 * One chunk's {@code yearofice:trees} (Chunk attachments; Trees, felling and firewood: "A standing tree is one record
 * in the chunk attachment"). Standing trees are TreePlan's and are never stored; this keeps only what a player
 * changed: each felled or topped tree as one {@link TreeMarks} long, filed by its base block. Saved as one long
 * array (8 bytes a tree), and nothing at all for a chunk no one has cut in.
 */
public final class TreeMarksData {
    private final Int2LongOpenHashMap marks = new Int2LongOpenHashMap();

    public synchronized void put(long mark) { marks.put(TreeMarks.key(mark), mark); }

    /** The mark at the base block (chunk-local x, z and y), or 0 when the tree is as planned. */
    public synchronized long get(int lx, int lz, int baseY) { return marks.getOrDefault(TreeMarks.key(lx, lz, baseY), 0L); }

    /** Forgets a mark (debug and test clean-up only). */
    public synchronized void remove(int lx, int lz, int baseY) { marks.remove(TreeMarks.key(lx, lz, baseY)); }

    public synchronized boolean has(int lx, int lz, int baseY) { return marks.containsKey(TreeMarks.key(lx, lz, baseY)); }

    public synchronized int size() { return marks.size(); }

    public synchronized boolean isEmpty() { return marks.isEmpty(); }

    /** Every mark, sorted by key so a save is byte-stable. */
    public synchronized long[] all() {
        int[] keys = marks.keySet().toIntArray();
        Arrays.sort(keys);
        long[] out = new long[keys.length];
        for (int i = 0; i < keys.length; i++) out[i] = marks.get(keys[i]);
        return out;
    }

    synchronized Tag write() { return marks.isEmpty() ? null : new LongArrayTag(all()); }

    static TreeMarksData read(Tag tag) {
        TreeMarksData d = new TreeMarksData();
        if (tag instanceof LongArrayTag a) for (long m : a.getAsLongArray()) d.put(m);
        return d;
    }

    /** The attachment's own tag, as the chunk save writes it (the budget test measures it). */
    public Tag saveTag() { return write(); }

    public static TreeMarksData load(Tag tag) { return read(tag); }
}
