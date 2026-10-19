package com.yearofice.gametest;

import com.yearofice.core.YearOfIce;
import com.yearofice.core.hash.Mulberry32;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Batch m0: proves the mod boots in a real server and the seeded RNG is the one the sim uses. */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/empty3", batch = "m0")
    public static void boot_rng_matches_reference(GameTestHelper helper) {
        long first = new Mulberry32(1).nextUInt();
        helper.assertTrue(first == 2693262067L, "Mulberry32(1) first value was " + first);
        helper.succeed();
    }
}
