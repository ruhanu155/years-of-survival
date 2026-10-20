package com.yearofice.body;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** {@code yearofice_run}: the Hardcore end flag of this world (Collapse and SOS, Hardcore). */
public final class RunState extends SavedData {
    public static final String ID = "yearofice_run";

    public boolean ended;
    public String cause = "";
    public long minute;

    public static RunState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RunState::new, RunState::load, null), ID);
    }

    static RunState load(CompoundTag t, HolderLookup.Provider r) {
        RunState s = new RunState();
        s.ended = t.getBoolean("ended");
        s.cause = t.getString("cause");
        s.minute = t.getLong("minute");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putBoolean("ended", ended);
        t.putString("cause", cause);
        t.putLong("minute", minute);
        return t;
    }

    public void end(String why, long atMinute) {
        ended = true;
        cause = why;
        minute = atMinute;
        setDirty();
        YearOfIce.LOG.info("Run ended: {} at minute {}", why, atMinute);
    }
}
