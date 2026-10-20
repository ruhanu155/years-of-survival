package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FishDataTest {
    static FishData load(String id) throws IOException {
        try (Reader r = new InputStreamReader(FishDataTest.class.getResourceAsStream("/data/yearofice/fish/" + id + ".json"), StandardCharsets.UTF_8)) {
            return FishData.parse(JsonParser.parseReader(r).getAsJsonObject());
        }
    }

    static void same(FishSpecies a, FishSpecies b) {
        assertEquals(a.id(), b.id());
        assertEquals(a.kgMin(), b.kgMin());
        assertEquals(a.kgMax(), b.kgMax());
        assertEquals(a.bitesPerMin(), b.bitesPerMin());
        assertEquals(a.kcalPerKg(), b.kcalPerKg());
        assertEquals(a.runPullX(), b.runPullX());
        assertArrayEquals(a.runEveryS(), b.runEveryS());
        assertArrayEquals(a.runLengthS(), b.runLengthS());
        assertEquals(a.jumpChance(), b.jumpChance());
        assertEquals(a.special(), b.special());
        assertEquals(a.tirePerS(), b.tirePerS(), 1e-6);
        assertEquals(a.biteThroughNoLeader(), b.biteThroughNoLeader());
        assertEquals(a.nightFactor(), b.nightFactor());
        assertEquals(a.bait(), b.bait());
    }

    @Test
    void filesMatchTheBlueprintReferenceSpecies() throws IOException {
        same(FishSpecies.arcticGrayling(), load("arctic_grayling").species());
        same(FishSpecies.northernPike(), load("northern_pike").species());
        same(FishSpecies.burbot(), load("burbot").species());
        same(FishSpecies.lakeTrout(), load("lake_trout").species());
        same(FishSpecies.humpbackWhitefish(), load("humpback_whitefish").species());
    }

    @Test
    void seasonsFollowTheLedger() throws IOException {
        FishSeason chinook = load("chinook_salmon").season();
        assertTrue(chinook.inWindow(FishSeason.day(7, 1)) && !chinook.inWindow(FishSeason.day(8, 5)));
        assertEquals(0.0, chinook.factor(FishSeason.day(1, 5), "outside_bend"));
        FishSeason gray = load("arctic_grayling").season();
        assertEquals(FishSeason.arcticGrayling(), gray);
    }

    @Test
    void missingKeyAndCamelCaseAreRejected() {
        JsonObject o = JsonParser.parseString("{\"id\":\"x\"}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> FishData.parse(o));
    }

    @Test
    void pikeSizeDrawMeanAtFullStock() throws IOException {
        FishSpecies pike = load("northern_pike").species();
        double sum = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            double kg = SizeDraw.kg(pike, 1.0, 1.0, 0.3, Draws.u(7, i, 0, 6), 1.0, 0.5);
            assertTrue(kg >= 1 && kg <= 10);
            sum += kg;
        }
        assertEquals(1 + 9 / 3.0, sum / n, 0.2); // E[u^2] = 1/3 gives 4.0 kg
    }
}
