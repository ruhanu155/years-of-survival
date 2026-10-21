package com.yearofice.world.terrain;

import com.yearofice.api.world.ChangeOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData {@code yearofice_terrain} (one per level): pending {@link ChangeOp}s keyed by chunk, the day each
 * chunk's passes last settled, and the flood water each chunk holds ({@code flood_flags}: exactly the positions the
 * flood placed, so the fall removes exactly those). The only writer is {@code world.terrain}.
 */
public final class TerrainChangeQueue extends SavedData {
    public static final String ID = "yearofice_terrain";
    static final SavedData.Factory<TerrainChangeQueue> FACTORY = new SavedData.Factory<>(TerrainChangeQueue::new, TerrainChangeQueue::load, null);

    final Map<Long, List<ChangeOp>> ops = new HashMap<>();
    final Map<Long, Long> stamps = new HashMap<>();
    final Map<Long, List<Long>> flood = new HashMap<>();

    public static TerrainChangeQueue get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, ID); }

    public void submit(ChangeOp op) {
        ops.computeIfAbsent(op.chunk().toLong(), k -> new ArrayList<>()).add(op);
        setDirty();
    }

    public int pending(ChunkPos c) { return ops.getOrDefault(c.toLong(), List.of()).size(); }

    public java.util.Set<Long> pendingChunks() { return ops.keySet(); }

    public int totalPending() { return ops.values().stream().mapToInt(List::size).sum(); }

    /** Pending ops of a chunk with {@code day <= today}, canonical order; not removed. */
    public List<ChangeOp> due(ChunkPos c, long today) {
        List<ChangeOp> out = new ArrayList<>();
        for (ChangeOp o : ops.getOrDefault(c.toLong(), List.of())) if (o.day() <= today) out.add(o);
        java.util.Collections.sort(out);
        return out;
    }

    public void done(ChunkPos c, List<ChangeOp> applied) {
        List<ChangeOp> l = ops.get(c.toLong());
        if (l == null) return;
        l.removeAll(applied);
        if (l.isEmpty()) ops.remove(c.toLong());
        setDirty();
    }

    public long stamp(ChunkPos c) { return stamps.getOrDefault(c.toLong(), Long.MIN_VALUE); }

    public void setStamp(ChunkPos c, long day) { stamps.put(c.toLong(), day); setDirty(); }

    public List<Long> floodFlags(ChunkPos c) { return flood.getOrDefault(c.toLong(), List.of()); }

    public void setFloodFlags(ChunkPos c, List<Long> flags) {
        if (flags.isEmpty()) flood.remove(c.toLong()); else flood.put(c.toLong(), new ArrayList<>(flags));
        setDirty();
    }

    public static TerrainChangeQueue load(CompoundTag tag, HolderLookup.Provider regs) {
        TerrainChangeQueue q = new TerrainChangeQueue();
        for (Tag t : tag.getList("ops", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            q.submit(new ChangeOp(c.getLong("day"), ChangeOp.Kind.valueOf(c.getString("kind")), c.getInt("prio"),
                    BlockPos.of(c.getLong("pos")), c.getString("block"), c.getBoolean("air_only"), c.getInt("seq")));
        }
        for (Tag t : tag.getList("stamps", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            q.stamps.put(c.getLong("chunk"), c.getLong("day"));
        }
        for (Tag t : tag.getList("flood", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            List<Long> l = new ArrayList<>();
            for (long v : c.getLongArray("pos")) l.add(v);
            q.flood.put(c.getLong("chunk"), l);
        }
        q.setDirty(false);
        return q;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        ListTag o = new ListTag();
        for (List<ChangeOp> l : ops.values()) {
            for (ChangeOp op : l) {
                CompoundTag c = new CompoundTag();
                c.putLong("day", op.day());
                c.putString("kind", op.kind().name());
                c.putInt("prio", op.priority());
                c.putLong("pos", op.pos().asLong());
                c.putString("block", op.block());
                c.putBoolean("air_only", op.airOnly());
                c.putInt("seq", op.seq());
                o.add(c);
            }
        }
        tag.put("ops", o);
        ListTag s = new ListTag();
        for (Map.Entry<Long, Long> e : stamps.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("chunk", e.getKey());
            c.putLong("day", e.getValue());
            s.add(c);
        }
        tag.put("stamps", s);
        ListTag f = new ListTag();
        for (Map.Entry<Long, List<Long>> e : flood.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("chunk", e.getKey());
            c.putLongArray("pos", e.getValue());
            f.add(c);
        }
        tag.put("flood", f);
        return tag;
    }
}
