package com.yearofice.worldgen.structure;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The {@code placed_mask}: one bit per block a set-piece template placed (Minecraft development › Processors ›
 * {@code yearofice:mark_placed}). World edits and the terrain-change queue (M7–M9) ask {@link #mayChange} before
 * they touch a block, so erosion, floods and slumps never wear a set piece away. Stored per level as SavedData
 * {@code yearofice_placed_mask}, a bit set per 16³ section.
 */
public final class PlacedMask extends SavedData {
    public static final String NAME = "yearofice_placed_mask";

    private final Long2ObjectOpenHashMap<BitSet> sections = new Long2ObjectOpenHashMap<>();

    public static PlacedMask get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PlacedMask::new, PlacedMask::load), NAME);
    }

    private static int bit(BlockPos p) {
        return ((p.getY() & 15) << 8) | ((p.getZ() & 15) << 4) | (p.getX() & 15);
    }

    public void mark(BlockPos pos) {
        sections.computeIfAbsent(SectionPos.asLong(pos), k -> new BitSet(4096)).set(bit(pos));
        setDirty();
    }

    public boolean isPlaced(BlockPos pos) {
        BitSet b = sections.get(SectionPos.asLong(pos));
        return b != null && b.get(bit(pos));
    }

    /** False for any block a set piece placed: terrain processes must leave it alone. */
    public boolean mayChange(BlockPos pos) { return !isPlaced(pos); }

    public int count() {
        int c = 0;
        for (BitSet b : sections.values()) c += b.cardinality();
        return c;
    }

    /** Forgets a box (debug and GameTest clean-up only). */
    public void clear(BlockPos min, BlockPos max) {
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BitSet b = sections.get(SectionPos.asLong(p));
            if (b != null) b.clear(bit(p));
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        long[] keys = sections.keySet().toLongArray();
        java.util.Arrays.sort(keys);
        for (long k : keys) {
            BitSet b = sections.get(k);
            if (b.isEmpty()) continue;
            CompoundTag s = new CompoundTag();
            s.putLong("section", k);
            s.put("bits", new LongArrayTag(b.toLongArray()));
            list.add(s);
        }
        tag.put("sections", list);
        return tag;
    }

    static PlacedMask load(CompoundTag tag, HolderLookup.Provider registries) {
        PlacedMask m = new PlacedMask();
        ListTag list = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            m.sections.put(s.getLong("section"), BitSet.valueOf(s.getLongArray("bits")));
        }
        return m;
    }
}
