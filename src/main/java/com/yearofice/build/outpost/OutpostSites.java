package com.yearofice.build.outpost;

import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Where each generated outpost stands (world saved data; this package is its one writer): origin corner to the
 * {@code OutpostDef} id, recorded when worldgen stamps it. A hold-to-work on any block in a site's box starts a
 * {@link RepairJob} over the pieces that are missing, which spends their {@code piece.json} materials.
 */
public final class OutpostSites extends SavedData {
    static final SavedData.Factory<OutpostSites> FACTORY = new SavedData.Factory<>(OutpostSites::new, OutpostSites::load, null);
    private final Map<Long, String> sites = new LinkedHashMap<>();

    public static OutpostSites get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, "yearofice_outpost_sites"); }

    static void record(ServerLevel level, BlockPos origin, String id) {
        OutpostSites s = get(level);
        s.sites.put(origin.asLong(), id);
        s.setDirty();
    }

    /** Drops a record (a GameTest's own site). */
    static void forget(ServerLevel level, BlockPos origin) {
        OutpostSites s = get(level);
        if (s.sites.remove(origin.asLong()) != null) s.setDirty();
    }

    /** The origins and ids recorded so far. */
    public Map<BlockPos, String> all() {
        Map<BlockPos, String> m = new LinkedHashMap<>();
        sites.forEach((k, v) -> m.put(BlockPos.of(k), v));
        return m;
    }

    /** The recorded outpost whose box holds {@code pos}, as origin to id, or null. */
    public Map.Entry<BlockPos, String> at(BlockPos pos) {
        for (Map.Entry<BlockPos, String> e : all().entrySet()) {
            OutpostDef def = OutpostDef.get(e.getValue());
            BlockPos o = e.getKey();
            if (pos.getX() >= o.getX() && pos.getX() < o.getX() + def.size()[0] && pos.getY() >= o.getY() && pos.getY() < o.getY() + def.size()[1]
                    && pos.getZ() >= o.getZ() && pos.getZ() < o.getZ() + def.size()[2]) return e;
        }
        return null;
    }

    /** A repair job for the outpost around the worked block, or null (not an outpost, or nothing to repair). */
    public static WorkJob repairAt(ServerPlayer p, BlockPos pos) {
        Map.Entry<BlockPos, String> e = get(p.serverLevel()).at(pos);
        if (e == null) return null;
        var miss = Outposts.missing(p.serverLevel(), e.getKey(), OutpostDef.get(e.getValue()));
        return miss.isEmpty() ? null : new RepairJob(p.serverLevel(), miss, true);
    }

    static void init() {
        var prev = WorkService.resolver;
        WorkService.resolver = (p, w) -> {
            WorkJob j = w.pos() == null ? null : repairAt(p, w.pos());
            return j != null ? j : prev.apply(p, w);
        };
    }

    static OutpostSites load(CompoundTag tag, HolderLookup.Provider regs) {
        OutpostSites s = new OutpostSites();
        for (Tag t : tag.getList("sites", Tag.TAG_COMPOUND)) s.sites.put(((CompoundTag) t).getLong("o"), ((CompoundTag) t).getString("id"));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        ListTag l = new ListTag();
        sites.forEach((k, v) -> { CompoundTag c = new CompoundTag(); c.putLong("o", k); c.putString("id", v); l.add(c); });
        tag.put("sites", l);
        return tag;
    }
}
