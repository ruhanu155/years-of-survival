package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** M0: changing an erosion parameter leaves the landform (stage 2) hash unchanged, and does change stage 3. */
class ErosionIsolationTest {
    @Test
    void erosionParamsNeverReachTheLandform() {
        long seed = 1L;
        Fixture.Summary base = Fixture.of(seed);
        WorldgenParams p = Fixture.PIPE.params();
        Archetype.Params[] arch = Archetype.loadAll();

        Pipeline fewerDroplets = new Pipeline(p.withErosion(p.erosion().withDroplets(30_000)), arch);
        Pipeline.Result r = fewerDroplets.attempt(seed, base.attempt());
        assertEquals(base.landformHash(), r.landformHash(), "landform hash moved with droplet count");
        assertNotEquals(base.heightHash(), r.heightHash(), "erosion change had no effect");

        Pipeline noThermal = new Pipeline(p.withErosion(p.erosion().withThermalPasses(0)), arch);
        SeedStage.SeedParams s = noThermal.seed(seed, base.attempt());
        assertEquals(base.landformHash(), noThermal.landform(s).hash(), "landform hash moved with thermal passes");
    }
}
