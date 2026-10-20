package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.SalmonRun;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What the player took from each run, by species and year, and the run sizes that follow (Fishing in depth ›
 * Salmon runs: R = R_mean · y · e). The harvest share {@code h} of a year's run sets the echo {@code e} of the run
 * that returns {@code returnsAfter} years later. Kept in the overworld's saved data.
 */
public final class RunLedger extends SavedData {
    static final SavedData.Factory<RunLedger> FACTORY = new SavedData.Factory<>(RunLedger::new, RunLedger::load, null);

    private final Map<String, Integer> taken = new HashMap<>();

    public static RunLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "yearofice_run_ledger");
    }

    /** The game year (0 for the first) of an absolute day count that starts at 1. */
    public static int yearOf(long absoluteDay) { return (int) Math.max(0, Math.floorDiv(absoluteDay - 1, 365L)); }

    static String key(String species, int year) { return species + "@" + year; }

    /** Counts {@code n} fish of a run species taken in a year, by any gear. Other species are ignored. */
    public void record(String species, int year, int n) {
        if (!SalmonRun.isRun(species) || n <= 0) return;
        taken.merge(key(species, year), n, Integer::sum);
        setDirty();
    }

    public int taken(String species, int year) { return taken.getOrDefault(key(species, year), 0); }

    /** y · e for a year's run: the year factor times the echo of the harvest {@code returnsAfter} years earlier. */
    public double strengthFactor(long seed, String species, int year) {
        SalmonRun.Run r = SalmonRun.RUNS.get(species);
        if (r == null) return 1;
        int back = year - r.returnsAfter();
        double echo = back < 0 ? 1.0 : FishStock.echo(share(seed, species, back));
        return SalmonRun.yearFactor(seed, species, year) * echo;
    }

    /** R, fish passing within reach, for a year. */
    public double runSize(long seed, String species, int year) {
        SalmonRun.Run r = SalmonRun.RUNS.get(species);
        return r == null ? 0 : r.rMean() * strengthFactor(seed, species, year);
    }

    /** The harvest share h of a year's run: the fish taken over R. */
    public double share(long seed, String species, int year) {
        double r = runSize(seed, species, year);
        return r <= 0 ? 0 : taken(species, year) / r;
    }

    public static RunLedger load(CompoundTag tag, HolderLookup.Provider regs) {
        RunLedger l = new RunLedger();
        CompoundTag t = tag.getCompound("taken");
        for (String k : t.getAllKeys()) l.taken.put(k, t.getInt(k));
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        taken.forEach(t::putInt);
        tag.put("taken", t);
        return tag;
    }

    /** y · e now, for the stocks of a run species (1 for any other). */
    public static double strengthNow(ServerLevel level, String species, long absoluteDay) {
        if (!SalmonRun.isRun(species) || !FishingSpecies.has(species) || FishingSpecies.season(species).resident()) return 1;
        return get(level).strengthFactor(level.getSeed(), species, yearOf(absoluteDay));
    }
}
