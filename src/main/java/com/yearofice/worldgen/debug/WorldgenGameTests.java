package com.yearofice.worldgen.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.Archetype;
import com.yearofice.worldgen.Landform;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.SeedStage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Batch m0, worldgen: inside a real server, the mod jar's worldgen data loads through the mod class loader and
 * stage 2 gives the same landform hash JUnit records in {@code HeightHashTest} (seed 3141592).
 */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private WorldgenGameTests() {}

    static final long SEED = 3141592L;
    static final int LANDFORM_HASH = 0x95b627d5;

    @GameTest(template = "gametest/empty3", batch = "m0")
    public static void worldgen_landform_hash_matches_junit(GameTestHelper helper) {
        Pipeline pipe = Pipeline.standard();
        SeedStage.SeedParams s = pipe.seed(SEED, 0);
        helper.assertTrue(s.archetype() == Archetype.OXBOW_MARSH, "seed " + SEED + " archetype " + s.archetype());
        helper.assertTrue(s.check(pipe.params()).isEmpty(), "stage 1 check: " + s.check(pipe.params()));
        Landform.Layers land = pipe.landform(s);
        helper.assertTrue(land.hash() == LANDFORM_HASH,
                "landform hash " + Integer.toHexString(land.hash()) + " != " + Integer.toHexString(LANDFORM_HASH));
        helper.succeed();
    }
}
