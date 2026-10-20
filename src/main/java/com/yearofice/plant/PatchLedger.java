package com.yearofice.plant;

import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.plant.PatchModel;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Reads and writes the {@code patches} chunk attachment (Plants and fungi › Regrowth and patch health). */
public final class PatchLedger {
    private PatchLedger() {}

    public static PatchData data(LevelChunk c) { return c.getData(PlantAttachments.PATCHES.get()); }

    /** The patch of {@code spec} in the chunk of {@code pos}, created on first touch and rolled over to the current year. */
    public static PatchData.Patch patch(ServerLevel level, BlockPos pos, PlantSpec spec) {
        LevelChunk c = level.getChunkAt(pos);
        PatchData d = data(c);
        int year = ClockService.get().year();
        PatchData.Patch p = d.patches.get(spec.id);
        if (p == null) {
            p = new PatchData.Patch();
            p.seasonYear = year;
            p.s = 0.8 + 0.4 * new Random(spec.id.hashCode() * 31L + c.getPos().x * 7919L + c.getPos().z * 104729L).nextDouble();
            p.cap = Math.max(1, count(level, c, pos.getY(), spec)) * primaryUnits(spec);
            d.patches.put(spec.id, p);
        } else {
            rollover(p, year);
        }
        c.setUnsaved(true);
        return p;
    }

    static double primaryUnits(PlantSpec spec) {
        PlantSpec.Verb v = spec.verbs.get("pick");
        if (v == null) v = spec.verbs.get("dig");
        if (v == null) v = spec.verbs.get("cut");
        return v == null ? 1.0 : v.unitsPerClump();
    }

    /** Each May 1 the picked share sets next year's health: over the threshold H is 0.5, otherwise it climbs by 0.25. */
    public static void rollover(PatchData.Patch p, int year) {
        double thr = ForageConfig.tuning().strippedThreshold();
        for (int y = p.seasonYear; y < year; y++) {
            double share = y == p.seasonYear && p.cap > 0 ? Math.min(1.0, p.picked / p.cap) : 0.0;
            p.h = PatchModel.nextHealth(p.h, share, thr, false);
            p.picked = 0;
        }
        p.seasonYear = Math.max(p.seasonYear, year);
    }

    static int count(ServerLevel level, LevelChunk c, int y, PlantSpec spec) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x0 = c.getPos().getMinBlockX(), z0 = c.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) for (int dy = -6; dy <= 6; dy++) {
            m.set(x0 + dx, y + dy, z0 + dz);
            if (level.getBlockState(m).getBlock() instanceof PlantBlock pb && pb.species().equals(spec.id)) n++;
        }
        return n;
    }

    /** Picked share of the patch this season, 0-1. */
    public static double pickedShare(PatchData.Patch p) { return p.cap <= 0 ? 0 : Math.min(1.0, p.picked / p.cap); }
}
