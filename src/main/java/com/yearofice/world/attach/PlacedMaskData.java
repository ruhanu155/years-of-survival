package com.yearofice.world.attach;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.BitSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;

/**
 * One chunk's {@code yearofice:placed_mask} (Chunk attachments): a 4,096-bit set per section that holds any
 * placed block, by section index (block y >> 4). Stored only for touched sections, so an untouched chunk saves
 * nothing. Synchronised: set-piece pieces mark it from world-generation workers.
 */
public final class PlacedMaskData {
    private final Int2ObjectOpenHashMap<BitSet> sections = new Int2ObjectOpenHashMap<>();

    static int bit(int x, int y, int z) { return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15); }

    public synchronized boolean mark(int x, int y, int z) {
        BitSet b = sections.computeIfAbsent(y >> 4, k -> new BitSet(4096));
        int i = bit(x, y, z);
        if (b.get(i)) return false;
        b.set(i);
        return true;
    }

    public synchronized boolean clear(int x, int y, int z) {
        BitSet b = sections.get(y >> 4);
        if (b == null || !b.get(bit(x, y, z))) return false;
        b.clear(bit(x, y, z));
        if (b.isEmpty()) sections.remove(y >> 4);
        return true;
    }

    public synchronized boolean isPlaced(int x, int y, int z) {
        BitSet b = sections.get(y >> 4);
        return b != null && b.get(bit(x, y, z));
    }

    public synchronized int count() {
        int c = 0;
        for (BitSet b : sections.values()) c += b.cardinality();
        return c;
    }

    public synchronized boolean isEmpty() { return sections.isEmpty(); }

    synchronized Tag write() {
        if (sections.isEmpty()) return null;
        ListTag list = new ListTag();
        int[] keys = sections.keySet().toIntArray();
        Arrays.sort(keys);
        for (int k : keys) {
            CompoundTag s = new CompoundTag();
            s.putInt("y", k);
            s.put("bits", new LongArrayTag(sections.get(k).toLongArray()));
            list.add(s);
        }
        CompoundTag t = new CompoundTag();
        t.put("sections", list);
        return t;
    }

    static PlacedMaskData read(Tag tag) {
        PlacedMaskData d = new PlacedMaskData();
        if (tag instanceof CompoundTag c) {
            ListTag list = c.getList("sections", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag s = list.getCompound(i);
                BitSet b = BitSet.valueOf(s.getLongArray("bits"));
                if (!b.isEmpty()) d.sections.put(s.getInt("y"), b);
            }
        }
        return d;
    }
}
