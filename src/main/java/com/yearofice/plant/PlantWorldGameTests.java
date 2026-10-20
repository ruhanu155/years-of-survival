package com.yearofice.plant;

import com.yearofice.sim.plant.PlantHabitat;
import com.yearofice.world.gametest.M2WorldGameTests;
import com.yearofice.worldgen.PlantPlan;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** The vegetation pass on real blocks (batch {@code m5}): the slice valley's plants stand where their files say. */
public final class PlantWorldGameTests {
    private PlantWorldGameTests() {}

    /**
     * {@code worldgen/plants_placed_by_habitat}: 8 x 8 chunks around the slice's start and 8 x 8 forty blocks away
     * generate to FULL (a saved slice world from an older build keeps its old chunks: delete run/world/dimensions/yearofice/slice_test after a worldgen change). Every plant block stands on a soil column whose cover class fits one of its species' habitat
     * tags, at least 150 clumps of at least 6 species are there, and the start site keeps a berry bush and a greens
     * patch within 50 blocks.
     */
    static void plantsPlacedByHabitat(GameTestHelper h) {
        ServerLevel l = h.getLevel().getServer().getLevel(M2WorldGameTests.SLICE);
        if (l == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no yearofice:slice_test dimension");
        ValleyWorld w = ((YoiChunkGenerator) l.getChunkSource().getGenerator()).bind(l.getSeed());
        int[] s = w.spawn();
        var entries = PlantHabitat.load(PlantCatalog.SPECIES);
        TreeSet<String> kinds = new TreeSet<>();
        int total = 0;
        boolean berry = false, greens = false;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int[] at : new int[][] {{s[0], s[2]}, {s[0] + 40, s[2] + 40}}) {
            int cx0 = (at[0] >> 4) - 4, cz0 = (at[1] >> 4) - 4;
            for (int cz = cz0; cz < cz0 + 8; cz++) for (int cx = cx0; cx < cx0 + 8; cx++) l.getChunk(cx, cz);
            int x0 = cx0 * 16, z0 = cz0 * 16, n = 128;
            ValleyWorld.Columns col = w.columns(x0, z0, n, n);
            for (int i = 0; i < n * n; i++) {
                int x = x0 + i % n, z = z0 + i / n;
                int y = col.ground[i] + 1;
                if (!(l.getBlockState(mp.set(x, y, z)).getBlock() instanceof PlantBlock pb)) continue;
                total++;
                kinds.add(pb.species());
                double d = Math.hypot(x - w.start().x(), z - w.start().z());
                if (d <= 50 && pb.species().endsWith("blueberry_bush")) berry = true;
                if (d <= 50 && (pb.species().endsWith("fireweed") || pb.species().endsWith("dandelion"))) greens = true;
                if (PlantPlan.START_PATCH_IDS.contains(pb.species()) && d <= 50) continue;
                PlantHabitat.Site site = PlantPlan.site(w, x, z);
                PlantHabitat.Site loose = new PlantHabitat.Site(site.cls(), 10, true, true, site.startDistM());
                var e = entries.stream().filter(en -> en.id().equals(pb.species())).findFirst().orElseThrow();
                h.assertTrue(PlantHabitat.fitsAny(e, loose), pb.species() + " stands on " + site.cls() + " at " + x + "," + z);
            }
        }
        h.assertTrue(total >= 150, "only " + total + " plant clumps");
        h.assertTrue(kinds.size() >= 6, "only " + kinds.size() + " species: " + kinds);
        h.assertTrue(berry && greens, "start site lacks a berry (" + berry + ") or greens (" + greens + ") patch within 50 m");
        h.succeed();
    }
}
