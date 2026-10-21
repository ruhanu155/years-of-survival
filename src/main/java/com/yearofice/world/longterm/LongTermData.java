package com.yearofice.world.longterm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** SavedData {@code yearofice_longterm} (one per level): beaver colonies and the dam stage each has reached. Writer: {@code world.longterm}. */
public final class LongTermData extends SavedData {
    public static final String ID = "yearofice_longterm";
    static final SavedData.Factory<LongTermData> FACTORY = new SavedData.Factory<>(LongTermData::new, LongTermData::load, null);

    public static final class Colony {
        public BlockPos pos;
        public int stage;
        public long lastGrowDay;
        public boolean abandoned;

        public Colony(BlockPos pos, int stage, long lastGrowDay, boolean abandoned) {
            this.pos = pos; this.stage = stage; this.lastGrowDay = lastGrowDay; this.abandoned = abandoned;
        }
    }

    public final List<Colony> colonies = new ArrayList<>();

    public static LongTermData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, ID); }

    public static LongTermData load(CompoundTag tag, HolderLookup.Provider regs) {
        LongTermData d = new LongTermData();
        for (Tag t : tag.getList("colonies", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.colonies.add(new Colony(BlockPos.of(c.getLong("pos")), c.getInt("stage"), c.getLong("last_grow_day"), c.getBoolean("abandoned")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        ListTag l = new ListTag();
        for (Colony c : colonies) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", c.pos.asLong());
            t.putInt("stage", c.stage);
            t.putLong("last_grow_day", c.lastGrowDay);
            t.putBoolean("abandoned", c.abandoned);
            l.add(t);
        }
        tag.put("colonies", l);
        return tag;
    }

    public void touch() { setDirty(); }
}
