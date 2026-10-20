package com.yearofice.sim.env;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yearofice.sim.env.HeldLightMath.Source;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Held light as gameplay light (Minecraft development › Gameplay light). */
class HeldLightMathTest {

    @Test
    void levelFallsOnePerBlock() {
        Source torch = Source.around(0.5, 64, 0.5, 10);
        assertEquals(10, HeldLightMath.at(torch, 0.5, 64, 0.5));
        assertEquals(9, HeldLightMath.at(torch, 1.5, 64, 0.5));
        assertEquals(4, HeldLightMath.at(torch, 6.5, 64, 0.5));
        assertEquals(0, HeldLightMath.at(torch, 10.5, 64, 0.5));
        assertEquals(0, HeldLightMath.at(torch, 40.5, 64, 0.5), "past the 16 block range");
    }

    @Test
    void theTableLevelsReachTheirRadii() {
        // Held lights: bark torch 10 (about 6 m of useful light), tin lantern 8, candle 6 (about 2 m, so level 4 there).
        assertEquals(4, HeldLightMath.at(Source.around(0, 0, 0, 10), 6, 0, 0));
        assertEquals(4, HeldLightMath.at(Source.around(0, 0, 0, 8), 4, 0, 0));
        assertEquals(4, HeldLightMath.at(Source.around(0, 0, 0, 6), 2, 0, 0));
    }

    @Test
    void aConeLightsOnlyInsideTheCone() {
        Source beam = new Source(0, 64, 0, 11, 30, 0, 0, 1);
        assertEquals(11 - 5, HeldLightMath.at(beam, 0, 64, 5), "straight ahead");
        assertEquals(0, HeldLightMath.at(beam, 5, 64, 0), "at the side");
        assertEquals(0, HeldLightMath.at(beam, 0, 64, -5), "behind");
        assertEquals(10, HeldLightMath.at(beam, 0.5, 64, -0.5), "at the player the cone does not apply");
    }

    @Test
    void theLargestSourceWinsAndOnlyEightCount() {
        List<Source> many = new ArrayList<>();
        for (int i = 0; i < 8; i++) many.add(Source.around(20 + i, 0, 0, 5));
        many.add(Source.around(0, 0, 0, 15));
        assertEquals(0, HeldLightMath.at(many, 0, 0, 0), "the ninth source is not counted");
        List<Source> two = List.of(Source.around(0, 0, 0, 6), Source.around(3, 0, 0, 10));
        assertEquals(10 - 1, HeldLightMath.at(two, 2, 0, 0));
    }
}
