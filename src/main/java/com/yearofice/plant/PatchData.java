package com.yearofice.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code yearofice:patches} (chunk attachment): per species in the chunk, the patch's seed value S, health H, units
 * picked this season and capacity, and per clump the day it comes back (the 0-8 day regrow count lives here, not in a
 * block state).
 */
public final class PatchData {
    public static final class Patch {
        public double s = 1.0, h = 1.0, picked, cap;
        public int lastDay = -1, seasonYear = 1;
    }

    record PRec(String species, double s, double h, double picked, double cap, int lastDay, int year) {
        static final Codec<PRec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("species").forGetter(PRec::species),
                Codec.DOUBLE.optionalFieldOf("s", 1.0).forGetter(PRec::s),
                Codec.DOUBLE.optionalFieldOf("h", 1.0).forGetter(PRec::h),
                Codec.DOUBLE.optionalFieldOf("picked", 0.0).forGetter(PRec::picked),
                Codec.DOUBLE.optionalFieldOf("cap", 0.0).forGetter(PRec::cap),
                Codec.INT.optionalFieldOf("last_day", -1).forGetter(PRec::lastDay),
                Codec.INT.optionalFieldOf("season_year", 1).forGetter(PRec::year)).apply(i, PRec::new));
    }

    record CRec(long pos, int ready) {
        static final Codec<CRec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(CRec::pos),
                Codec.INT.fieldOf("ready_day").forGetter(CRec::ready)).apply(i, CRec::new));
    }

    public static final Codec<PatchData> CODEC = RecordCodecBuilder.create(i -> i.group(
            PRec.CODEC.listOf().optionalFieldOf("patches", List.of()).forGetter(PatchData::patchRecs),
            CRec.CODEC.listOf().optionalFieldOf("clumps", List.of()).forGetter(PatchData::clumpRecs)).apply(i, PatchData::of));

    public final Map<String, Patch> patches = new LinkedHashMap<>();
    /** Clump position to the absolute day it regrows. */
    public final Map<Long, Integer> ready = new HashMap<>();

    private static PatchData of(List<PRec> ps, List<CRec> cs) {
        PatchData d = new PatchData();
        for (PRec r : ps) {
            Patch p = new Patch();
            p.s = r.s(); p.h = r.h(); p.picked = r.picked(); p.cap = r.cap(); p.lastDay = r.lastDay(); p.seasonYear = r.year();
            d.patches.put(r.species(), p);
        }
        for (CRec r : cs) d.ready.put(r.pos(), r.ready());
        return d;
    }

    private List<PRec> patchRecs() {
        List<PRec> out = new ArrayList<>();
        patches.forEach((k, p) -> out.add(new PRec(k, p.s, p.h, p.picked, p.cap, p.lastDay, p.seasonYear)));
        return out;
    }

    private List<CRec> clumpRecs() {
        List<CRec> out = new ArrayList<>();
        ready.forEach((k, v) -> out.add(new CRec(k, v)));
        return out;
    }
}
